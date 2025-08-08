import { ref, computed, onUnmounted, markRaw, watch } from 'vue'
import { Device } from 'mediasoup-client'
import { Deferred } from '../utils/deferred'

//#region 인터페이스 정의
export interface StompClient {
  connected: boolean
  connect: (url: string, token: string, userEmail?: string) => void
  disconnect: () => void
  subscribe: (destination: string, callback: (message: any) => void) => { unsubscribe: () => void } | null
  publish: (destination: string, body: any) => void
}

export interface AudioController {
  setLocalAudioTrack: (track: MediaStreamTrack | null) => void
  connectParticipantAudio: (userEmail: string, stream: MediaStream) => void
  cleanup: () => void
}

export interface UserInfo {
  email: string
  nickname: string
  isLoggedIn: boolean
}

export interface WebRTCConfig {
  wsBaseUrl?: string
  apiBaseUrl?: string
  isTestMode?: boolean
  testUser?: {
    email: string
    nickname: string
  }
}

// WebRTC 연결 옵션
export interface WebRTCConnectionOptions {
  stompClient?: StompClient
  audioController?: AudioController
  userInfo?: UserInfo
  config?: WebRTCConfig
  onConnectionStep?: (step: string, message: string) => void
  onParticipantUpdate?: (participants: WebRTCParticipant[]) => void
  onError?: (error: string) => void
  participantsCount?: number
}

export interface WebRTCParticipant {
  producerUserEmail: string
  producerId: string
  connected: boolean
  audioStream: MediaStream | null
  consumerTransport: any
  audioConsumer?: any
}

export interface WebRTCConnectionState {
  isConnecting: boolean
  isConnected: boolean
  connectionStep: 'auth' | 'router' | 'recv' | 'send' | 'consumer' | 'completed'
  connectionStepText: string
  participants: WebRTCParticipant[]
  device?: Device
  routerRtpCapabilities?: any
  producerTransport?: any
  consumerTransport?: any
  localAudioTrack?: MediaStreamTrack | null
}
//#endregion

//#region 헬퍼 함수: 고유 테스트 사용자 생성
function generateUniqueTestUser() {
  const sessionId = Math.random().toString(36).substring(2, 8)
  const baseEmail = import.meta.env.VITE_TEST_USER_EMAIL || 'test@example.com'
  const baseNickname = import.meta.env.VITE_TEST_USER_NICKNAME || 'TestUser'
  
  const emailParts = baseEmail.split('@')
  const uniqueEmail = `${emailParts[0]}_${sessionId}@${emailParts[1]}`
  const uniqueNickname = `${baseNickname}_${sessionId}`
  
  return { email: uniqueEmail, nickname: uniqueNickname }
}
//#endregion

export const useWebRTCConnection = (options: WebRTCConnectionOptions = {}) => {
  // 기본 설정
  const config: Required<WebRTCConfig> = {
    wsBaseUrl: options.config?.wsBaseUrl || import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080',
    apiBaseUrl: options.config?.apiBaseUrl || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    isTestMode: options.config?.isTestMode ?? (import.meta.env.VITE_TEST_MODE === 'true' || import.meta.env.NODE_ENV === 'development'),
    testUser: options.config?.testUser || generateUniqueTestUser(),
  }

  const expectedRemote = (options.participantsCount ?? 2) - 1

  // 각 단계 완료 신호
  const stepDone = {
    auth: new Deferred<void>(),
    routerReady: new Deferred<void>(),
    recvReady: new Deferred<void>(),
    sendReady: new Deferred<void>(),
    consumerReady: new Deferred<void>(),
    completed: new Deferred<void>(),
  } as const
  
  // 연결 단계별 메시지
  const connectionStepMessages = {
    auth: '서버 연결 중...',
    router: '라우터 정보 요청 중...',
    recv: '수신 준비 중...',
    send: '송신 준비 중...',
    consumer: '다른 참가자와 연결 중...',
    completed: '연결 완료!'
  } as const

  // 의존성들 (옵셔널)
  const stompClient = options.stompClient
  const audioController = options.audioController
  const externalUserInfo = options.userInfo

  // 콜백 함수들
  const onConnectionStep = options.onConnectionStep
  const onParticipantUpdate = options.onParticipantUpdate  
  const onError = options.onError
  
  // 기본 상태 관리
  const state = ref<WebRTCConnectionState>({
    isConnecting: false,
    isConnected: false,
    connectionStep: 'auth',
    connectionStepText: '인증 확인 중...',
    participants: [],
    device: undefined,
    routerRtpCapabilities: undefined,
    producerTransport: undefined,
    consumerTransport: undefined,
    localAudioTrack: null
  })

  const connectionError = ref<string | null>(null)
  const roomId = ref<string>('')

  // 계산된 속성들
  const allParticipantsConnected = computed(() => {
    const currentParticipants = state.value.participants.length
    return expectedRemote > 0 && currentParticipants === expectedRemote && state.value.participants.every(p => p.connected)
  })
  
  watch(
    () => ({
      total: state.value.participants.length,
      allConnected: allParticipantsConnected.value,
      expected: expectedRemote
    }
  ),
    ({ total, allConnected, expected }) => {
      console.log('🔄 참가자 수:', total, '연결 상태:', allConnected, '예상 참가자 수:', expected)
      if (allConnected && total === expected && state.value.connectionStep !== 'completed') {
        updateConnectionStep('completed')
        stepDone.consumerReady.resolve()
        state.value.isConnecting = false
        state.value.isConnected  = true
      }
    }
  )
  

  const connectionProgress = computed(() => {
    const steps = ['auth', 'router', 'recv', 'send', 'consumer', 'completed']
    const currentStepIndex = steps.indexOf(state.value.connectionStep)
    return Math.round((currentStepIndex / (steps.length - 1)) * 100)
  })

  const signalingUrl = computed(() => `${config.wsBaseUrl}/signaling`)
  
  // MediaSoup 관련 변수들
  const pendingTransportConnect = new Map<string, Deferred<void>>()
  const pendingProducerCompleted = new Map<string, Deferred<void>>()
  const pendingConsumerCompleted = new Map<string, Deferred<void>>()
  let produceA: string | null = null
  let produceV: string | null = null

  // 내부 STOMP 클라이언트 상태 (외부 클라이언트가 없을 때만 사용)
  const internalStompState = ref({
    connected: false,
    client: null as any,
    subscriptions: new Map<string, any>()
  })

  // 현재 사용자 정보 가져오기
  const getCurrentUser = (): UserInfo => {
    if (externalUserInfo) {
      return externalUserInfo
    }
    
    // 외부 사용자 정보가 없으면 기본값 사용
    if (config.isTestMode && config.testUser) {
      return {
        email: config.testUser.email,
        nickname: config.testUser.nickname,
        isLoggedIn: true
      }
    }
    
    // 기본 사용자 정보
    return {
      email: 'anonymous@example.com',
      nickname: 'Anonymous',
      isLoggedIn: false
    }
  }

  // 내부 STOMP 클라이언트 초기화
  const initializeInternalStompClient = async (url: string, token: string, userEmail: string): Promise<void> => {
    return new Promise((resolve, reject) => {
      try {
        // @stomp/stompjs Client 동적 import
        import('@stomp/stompjs').then(({ Client }) => {
          const client = new Client({
            brokerURL: url,
            connectHeaders: {
              'Authorization': `Bearer ${token}`,
              'login': userEmail
            },
            debug: (str) => {
              console.log('내부 STOMP Debug:', str)
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: (frame) => {
              internalStompState.value.connected = true
              console.log('✅ 내부 STOMP 연결됨:', frame)
              resolve()
            },
            onDisconnect: (frame) => {
              internalStompState.value.connected = false
              console.log('❌ 내부 STOMP 연결 해제됨:', frame)
            },
            onStompError: (frame) => {
              console.error('💥 내부 STOMP 오류:', frame)
              internalStompState.value.connected = false
              reject(new Error(`STOMP 오류: ${frame.headers.message}`))
            },
            onWebSocketError: (error) => {
              console.error('🔌 내부 WebSocket 오류:', error)
              internalStompState.value.connected = false
              reject(error)
            }
          })

          internalStompState.value.client = client
          client.activate()
        }).catch(reject)
      } catch (error) {
        reject(error)
      }
    })
  }

  // 내부 STOMP 클라이언트 래퍼 생성
  const createInternalStompWrapper = (): StompClient => {
    return {
      get connected() {
        return internalStompState.value.connected
      },
      connect: (url: string, token: string, userEmail?: string) => {
        if (!userEmail) {
          userEmail = getCurrentUser().email
        }
        // async 함수를 동기적으로 호출하되, 에러는 콘솔에 로그
        initializeInternalStompClient(url, token, userEmail).catch(error => {
          console.error('내부 STOMP 클라이언트 연결 실패:', error)
          handleError('내부 STOMP 클라이언트 연결 실패', error)
        })
      },
      disconnect: () => {
        if (internalStompState.value.client) {
          // 모든 구독 해제
          internalStompState.value.subscriptions.forEach(subscription => {
            try {
              subscription.unsubscribe()
            } catch (error) {
              console.warn('구독 해제 중 오류:', error)
            }
          })
          internalStompState.value.subscriptions.clear()
          
          // 클라이언트 비활성화 (실제 STOMP 클라이언트가 있는 경우만)
          if (internalStompState.value.client.deactivate) {
            try {
              internalStompState.value.client.deactivate()
            } catch (error) {
              console.warn('STOMP 클라이언트 비활성화 중 오류:', error)
            }
          }
          
          internalStompState.value.client = null
          internalStompState.value.connected = false
        }
      },
      subscribe: (destination: string, callback: (message: any) => void) => {
        if (internalStompState.value.client && 
            internalStompState.value.connected && 
            internalStompState.value.client.subscribe) {
          try {
            const subscription = internalStompState.value.client.subscribe(destination, callback)
            internalStompState.value.subscriptions.set(destination, subscription)
            return subscription
          } catch (error) {
            console.error('구독 중 오류:', error)
            return null
          }
        }
        console.warn('내부 STOMP 클라이언트가 연결되지 않음')
        return null
      },
      publish: (destination: string, body: any) => {
        if (internalStompState.value.client && 
            internalStompState.value.connected && 
            internalStompState.value.client.publish) {
          try {
            internalStompState.value.client.publish({
              destination,
              body: typeof body === 'string' ? body : JSON.stringify(body),
              headers: {
                'content-type': 'application/json'
              }
            })
          } catch (error) {
            console.error('메시지 발송 중 오류:', error)
          }
        } else {
          console.warn('내부 STOMP 클라이언트가 연결되지 않음')
        }
      }
    }
  }

  // STOMP 클라이언트 접근자 (외부 또는 내부)
  const getStompClient = (): StompClient | null => {
    if (stompClient) {
      return stompClient
    }
    
    // 내부 STOMP 클라이언트가 없으면 생성
    if (!internalStompState.value.client) {
      console.log('🔧 내부 STOMP 클라이언트 준비 중...')
      // 빈 클라이언트 객체 생성 (실제 연결은 connect() 호출 시)
      internalStompState.value.client = {}
    }
    
    return createInternalStompWrapper()
  }

  // 상태 업데이트 헬퍼
  const updateConnectionStep = <K extends keyof typeof connectionStepMessages>(key: K) => {
    state.value.connectionStep = key
    state.value.connectionStepText = connectionStepMessages[key]
    console.log(`🔄 연결 단계: ${key} - ${connectionStepMessages[key]}`)
    onConnectionStep?.(key, connectionStepMessages[key])
  }

  // 오류 처리 헬퍼
  const handleError = (message: string, error?: any) => {
    console.error(`❌ ${message}:`, error)
    connectionError.value = message
    onError?.(message)
  }

  // 참가자 업데이트 헬퍼
  const updateParticipants = () => {
    console.log('🔄 updateParticipants', state.value.participants)
    onParticipantUpdate?.(state.value.participants)
  }

  // 1. 인증 및 STOMP 연결
  const authenticateAndConnect = async (): Promise<boolean> => {    
    const client = getStompClient()
    if (!client) {
      throw new Error('STOMP 클라이언트가 제공되지 않았습니다.')
    }

    try {
      const userInfo = getCurrentUser()
      
      // 로그인 체크 (테스트 모드가 아닌 경우만)
      if (!config.isTestMode && !userInfo.isLoggedIn) {
        throw new Error('로그인이 필요합니다.')
      }
      
      // 토큰 요청
      const response = await fetch(`${config.apiBaseUrl}/api/auth/token`, {
        method: 'POST',
        mode: 'cors',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify({
          userEmail: userInfo.email,
        }),
      })
      
      const data = await response.json()
      const authToken = data.token

      // STOMP 연결
      client.connect(signalingUrl.value, authToken, userInfo.email)
      
      // 연결 대기 (최대 5초)
      let attempts = 0
      while (!client.connected && attempts < 50) {
        await new Promise(resolve => setTimeout(resolve, 100))
        attempts++
      }
      
      if (!client.connected) {
        throw new Error('STOMP 연결 시간 초과')
      }

      console.log('✅ 인증 및 STOMP 연결 완료')
      return true
    } catch (error) {
      handleError('인증 및 STOMP 연결 실패', error)
      throw error
    }
  }

  // 2. 라우터 정보 요청 및 Device 초기화
  const initializeDevice = async (): Promise<void> => {    
    const client = getStompClient()
    if (!client) {
      throw new Error('STOMP 클라이언트가 제공되지 않았습니다.')
    }
    
    return new Promise((resolve, reject) => {
      // Router 정보 구독
      const subscription = client.subscribe('/user/queue/router', async (message) => {
        try {
          const routerInfo = JSON.parse(message.body)
          console.log('📡 Router 정보 수신:', routerInfo)
          
          // Device 초기화
          state.value.device = markRaw(new Device())
          await state.value.device.load({ routerRtpCapabilities: routerInfo.rtpCapabilities })
          state.value.routerRtpCapabilities = routerInfo.rtpCapabilities
          
          // 기존 참가자들 정보 저장
          if (routerInfo.participants && Array.isArray(routerInfo.participants)) {
            state.value.participants = routerInfo.participants.map((participant: any) => ({
              producerUserEmail: participant.producerUserEmail,
              producerId: participant.producerId,
              connected: false,
              audioStream: null,
              consumerTransport: null,
            }))
            console.log('👥 기존 참가자들 정보 저장:', state.value.participants)
            updateParticipants()
          }
          
          console.log('✅ Device 초기화 완료')
          subscription?.unsubscribe()
          stepDone.routerReady.resolve()
          resolve()
        } catch (error) {
          handleError('Router 정보 처리 실패', error)
          subscription?.unsubscribe()
          reject(error)
        }
      })
      
      // 타임아웃 설정
      setTimeout(() => {
        subscription?.unsubscribe()
        reject(new Error('Router 정보 수신 시간 초과'))
      }, 10000)
    })
  }

  // 3. Producer Transport 생성 및 연결
  const createProducerTransport = async (client: StompClient) => {    
    console.log('✅ createProducerTransport 호출')
    const data = await new Promise<any>((resolve) => {
      const sub = client.subscribe('/user/queue/transport', (msg) => {
        const d = JSON.parse(msg.body)
        console.log('✅ Producer Transport 생성 응답 수신:', d)
        if (d.type === 'producerTransportCreated') {
          sub?.unsubscribe()
          resolve(d)
        }
      })
      client.publish('/signaling/createTransport', { isProducer: true })
    })

    const transport = state.value.device!.createSendTransport({
      id: data.transportId,
      dtlsParameters: data.dtlsParameters,
      iceCandidates: data.iceCandidates,
      iceParameters: data.iceParameters,
    })
    state.value.producerTransport = markRaw(transport)
    console.log('✅ Producer Transport 객체 생성:', transport)
    return transport
  }

  const connectProducerTransport = async (client: StompClient, transport: any) => {
    console.log('✅ connectProducerTransport 호출')
    const transportConnectDeferred = new Deferred<void>()
    const producerCreateDeferred = new Deferred<void>()
    
    pendingTransportConnect.set(transport.id, transportConnectDeferred)
    
    transport.on('connect', ({dtlsParameters}: any, callback: any, errback: any) => {
      transportConnectDeferred.promise
        .then(() => { callback() })
        .catch(errback)

      client.publish('/signaling/connectTransport', { transportId: transport.id, dtlsParameters: dtlsParameters })
      console.log('✅ Producer Transport 연결 요청:', transport.id)
    })
    
    transport.on('produce', ({kind, rtpParameters}: any, callback: any, errback: any) => {
      // Transport 연결이 완료된 후 Producer 생성 콜백 실행
      transportConnectDeferred.promise
        .then(() => { 
          // 임시 Producer ID로 대기열에 등록 (서버 응답에서 실제 ID로 교체됨)
          pendingProducerCompleted.set(transport.id, producerCreateDeferred)
          callback({ id: transport.id })
        })
        .catch(errback)

      client.publish('/signaling/createProducer', { transportId: transport.id, kind: kind, rtpParameters: rtpParameters })
      console.log('✅ Producer 생성 요청:', kind)
    })
    
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: false,
    })
    const track = stream.getAudioTracks()[0]
    await transport.produce({ track })
    
    // Transport 연결과 Producer 생성 둘 다 완료될 때까지 기다림
    await Promise.all([transportConnectDeferred.promise, producerCreateDeferred.promise])
    
    return
  }

  // 4. Producer 생성 (로컬 오디오)
  const createAudioProducer = async (): Promise<MediaStreamTrack> => {
    console.log('✅ createAudioProducer 호출')
    if (!state.value.producerTransport) {
      throw new Error('Producer Transport가 생성되지 않았습니다')
    }
    
    try {
      const stream = markRaw(
        await navigator.mediaDevices.getUserMedia({
          audio: true,
          video: false,
        })
      )
      const audioTrack = stream.getAudioTracks()[0]
      state.value.localAudioTrack = audioTrack
      
      // Audio Controller에 트랙 설정 (옵셔널)
      audioController?.setLocalAudioTrack(audioTrack)
      
      await state.value.producerTransport.produce({
        track: audioTrack,
      })
      
      console.log('✅ 오디오 Producer 생성 완료')
      return audioTrack
    } catch (error) {
      handleError('오디오 Producer 생성 실패', error)
      throw error
    }
  }

  // Consumer Transport 생성 핸들러
  const handleConsumerTransportCreated = async (transportInfo: any) => {
    try {
      if (!state.value.device) {
        throw new Error('Device가 초기화되지 않았습니다')
      }

      const { transportId, dtlsParameters, iceCandidates, iceParameters, producerUserEmail } = transportInfo
      
      console.log(`🚛 Consumer Transport 생성: ${transportInfo}`)
      
      const transport = state.value.device.createRecvTransport({
        id: transportId,
        dtlsParameters,
        iceCandidates,
        iceParameters,
      })
      
      // 참가자 찾기
      const participant = state.value.participants.find(p => p.producerUserEmail === producerUserEmail)
      if (!participant) {
        console.warn(`참가자를 찾을 수 없음: ${producerUserEmail}`)
        return
      }
      
      participant.consumerTransport = markRaw(transport)
      
      transport.on('connect', ({ dtlsParameters }: any, callback: any, errback: any) => {
        const client = getStompClient()
        if (!client?.connected) {
          errback(new Error('STOMP 연결이 끊어졌습니다'))
          return
        }
        
        client.publish('/signaling/connectTransport', {
          transportId,
          dtlsParameters,
          producerUserEmail
        })
        
        // 즉시 콜백 호출 (서버 응답을 기다리지 않음)
        callback()
      })
      
      console.log(`✅ Consumer Transport 생성 완료: ${producerUserEmail}`)
      
      // Consumer 생성 요청
      const client = getStompClient()
      if (client?.connected) {
        client.publish('/signaling/createConsumer', {
          transportId,
          producerUserEmail,
          rtpCapabilities: state.value.device.rtpCapabilities
        })
      }
      
    } catch (error) {
      console.error('Consumer Transport 생성 실패:', error)
    }
  }
  
  // Consumer 생성 핸들러
  const handleConsumerCreated = async (consumerInfo: any) => {
    try {
      const { consumerId, producerId, kind, rtpParameters, producerUserEmail, transportId } = consumerInfo
      
      console.log(`🎧 Consumer 생성: ${producerUserEmail}, kind: ${kind}`)
      
      // 참가자 찾기
      const participant = state.value.participants.find(p => p.producerUserEmail === producerUserEmail)
      if (!participant || !participant.consumerTransport) {
        console.warn(`참가자 또는 Consumer Transport를 찾을 수 없음: ${producerUserEmail}`)
        return
      }
      
      // Consumer 생성
      const consumer = await participant.consumerTransport.consume({
        id: consumerId,
        producerId,
        kind,
        rtpParameters,
      })
      
      // Audio Stream 생성
      if (kind === 'audio') {
        const stream = new MediaStream([consumer.track])
        participant.audioStream = markRaw(stream)
        participant.audioConsumer = markRaw(consumer)
        participant.connected = true
        updateParticipants()
        
        // Audio Controller에 연결 (옵셔널)
        if (audioController) {
          audioController.connectParticipantAudio(participant.producerUserEmail, stream)
        }
        
        console.log(`🎵 오디오 Consumer 연결 완료: ${producerUserEmail}`)
      }
      
      // Consumer resume
      const client = getStompClient()
      if (client?.connected) {
        client.publish('/signaling/resumeConsumer', {
          consumerId,
          producerUserEmail
        })
      }
      
      updateParticipants()
      
    } catch (error) {
      console.error('Consumer 생성 실패:', error)
    }
  }
  
  // 새 참가자 처리
  const handleNewProducer = async (participantInfo: any) => {
    const { producerUserEmail, producerId } = participantInfo
    
    console.log(`🆕 새 발화자 추가: ${producerUserEmail}`)
    
    // 기존 발화자인지 확인
    const existingParticipant = state.value.participants.find(p => p.producerUserEmail === producerUserEmail)
    if (existingParticipant) {
      console.log('이미 존재하는 발화자입니다')
      return
    }
    
    // 새 발화자 추가
    const newParticipant: WebRTCParticipant = {
      producerUserEmail,
      producerId,
      connected: false,
      audioStream: null,
      consumerTransport: null,
      audioConsumer: null
    }
    
    state.value.participants.push(newParticipant)
    console.log("🔄 새 발화자 추가:", state.value.participants)

    // Consumer Transport 생성 요청
    const client = getStompClient() as StompClient
    await createConsumer(client, producerUserEmail, producerId)
    updateParticipants()
  }

    
  // 5. Consumer Transport

  const createConsumer = async (client: StompClient, producerUserEmail: string, producerId: string) => {
    console.log('✅ createConsumer 호출')
    const data = await new Promise<any>((resolve) => {
      const sub = client.subscribe('/user/queue/consumer', (msg) => {
        const d = JSON.parse(msg.body)
        console.log('✅ Consumer 생성 응답 수신:', d)
        sub?.unsubscribe()
        resolve(d)
      })
      client.publish('/signaling/createConsumer', { 
        transportId: state.value.consumerTransport?.id, 
        producerId: producerId,
        producerUserEmail: producerUserEmail, 
        rtpCapabilities: state.value.device?.rtpCapabilities
       })
       console.log('✅ Consumer 생성 요청:', {
        transportId: state.value.consumerTransport?.id,
        producerId: producerId,
        producerUserEmail: producerUserEmail,
        rtpCapabilities: state.value.device?.rtpCapabilities
       })
    })
    console.log("🔄 Consumer Transport 정보:", state.value.consumerTransport?.id)
    const consumer = await state.value.consumerTransport!.consume({
      id: data.consumerId,
      producerId: data.producerId,
      kind: data.kind,
      rtpParameters: data.rtpParameters,
    })

    if (data.kind === 'audio') {
      const stream = new MediaStream([consumer.track])
    
      const participant = state.value.participants
        .find(p => p.producerUserEmail === producerUserEmail)
    
      if (participant) {
        participant.audioStream = markRaw(stream)
        participant.audioConsumer = markRaw(consumer)
        participant.connected = true
      }

      console.log('🔄 참가자 연결 상태 업데이트:', participant)
    
      audioController?.connectParticipantAudio(producerUserEmail, stream)
    }

    console.log('✅ Consumer 생성 완료:', consumer)
    return consumer
  }

  // 5. Consumer Transport 및 Consumer 생성
  const connectConsumers = async (client: StompClient) => {
    console.log('✅ connectConsumers 호출')
    if (state.value.participants.length === 0) {
      console.log('참가자 대기중...')
      return
    }

    if (!client?.connected) {
      throw new Error('STOMP 클라이언트가 연결되지 않았습니다')
    }

    for (const participant of state.value.participants) {
      console.log('참가자 정보:', participant)
      const consumer = await createConsumer(client, participant.producerUserEmail, participant.producerId)
      console.log('✅ Consumer 생성 완료:', consumer)
    }
    return
  }

  // Recv 단계
  const createRecvTransport = async (client: StompClient) => {
    console.log('✅ createRecvTransport 호출')

    const data = await new Promise<any>((resolve) => {
      const sub = client.subscribe('/user/queue/transport', (msg) => {
        const d = JSON.parse(msg.body)
        console.log('✅ Recv Transport 생성 응답 수신:', d)

        if (d.type === 'consumerTransportCreated') {
          sub?.unsubscribe()
          stepDone.recvReady.resolve()
          resolve(d)
        }
      })
      client.publish('/signaling/createTransport', { producerUserEmail: '', isProducer: false })
    })

    const transport = state.value.device!.createRecvTransport({
      id: data.transportId,
      dtlsParameters: data.dtlsParameters,
      iceCandidates: data.iceCandidates,
      iceParameters: data.iceParameters,
    })

    const transportConnectDeferred = new Deferred<void>()
    pendingTransportConnect.set(transport.id, transportConnectDeferred)

    transport.on('connect', ({dtlsParameters}: any, callback: any, errback: any) => {
      transportConnectDeferred.promise
        .then(() => { 
          callback()
         })
        .catch(errback)

      console.log('✅ Recv Transport 연결 요청:', transport.id)

      client.publish('/signaling/connectTransport', { transportId: transport.id, dtlsParameters: dtlsParameters })
    })
    state.value.consumerTransport = markRaw(transport)
    console.log('✅ Recv Transport 객체 생성:', transport)
    return transport
  }

  const connectRecvTransport = async (client: StompClient, transport: any) => {
    console.log('✅ connectRecvTransport 호출')
    const transportConnectDeferred = new Deferred<void>()
    
    pendingTransportConnect.set(transport.id, transportConnectDeferred)
    
    transport.on('connect', ({dtlsParameters}: any, callback: any, errback: any) => {
      transportConnectDeferred.promise
        .then(() => { 
          callback()
         })
        .catch(errback)

      client.publish('/signaling/connectTransport', { transportId: transport.id, dtlsParameters: dtlsParameters })
      console.log('✅ Consumer Transport 연결 요청:', transport.id)
    })
  }

  // STOMP 메시지 핸들러 설정
  const setupStompMessageHandlers = (client: StompClient) => {
    console.log('🔧 STOMP 메시지 핸들러 설정 중...')
    
    // Transport 연결 완료 응답 처리
    client.subscribe('/user/queue/transport-connected', (message) => {
      const { connectedTransportId } = JSON.parse(message.body)
      pendingTransportConnect.get(connectedTransportId)?.resolve()
      pendingTransportConnect.delete(connectedTransportId)
      console.log('🔄 Transport 연결 콜백 실행 완료')
    })

    // Producer 생성 완료 응답 처리
    client.subscribe('/user/queue/producer', (message) => {
      const response = JSON.parse(message.body)
      console.log('🔄 Producer 생성 응답 수신:', response)
      const deferred = pendingProducerCompleted.get(state.value.producerTransport?.id)
      if (deferred) {
        deferred.resolve()
        pendingProducerCompleted.delete(state.value.producerTransport?.id)
        console.log('🔄 Producer 생성 콜백 실행 완료')
        stepDone.sendReady.resolve()
      } else {
        console.warn('Producer 대기열에서 찾을 수 없음:', state.value.producerTransport?.id)
      }
    })
    
    // Consumer 생성 응답 처리
    client.subscribe('/user/queue/consumer', (message) => {
      const { consumerId, producerId, kind, rtpParameters, producerUserEmail, transportId } = JSON.parse(message.body)
      pendingConsumerCompleted.get(consumerId)?.resolve()
      pendingConsumerCompleted.delete(consumerId)
      console.log('🔄 Consumer 생성 콜백 실행 완료')
    })
    
    // 새 발화자 입장 알림
    client.subscribe('/user/queue/new-producer', (message) => {
      const participantInfo = JSON.parse(message.body)
      console.log('🆕 새 발화자 입장:', participantInfo)
      handleNewProducer(participantInfo)
    })

    console.log('✅ STOMP 메시지 핸들러 설정 완료')

  }

  // 메인 WebRTC 연결 프로세스
  const startWebRTCConnection = async (targetRoomId: string): Promise<boolean> => {
    try {
      state.value.isConnecting = true
      state.value.isConnected = false
      connectionError.value = null
      roomId.value = targetRoomId
      
      console.log('🚀 WebRTC 연결 프로세스 시작')
      
      // 의존성 확인
      const client = getStompClient()
      if (!client) {
        throw new Error('STOMP 클라이언트가 제공되지 않았습니다. options.stompClient를 설정해주세요.')
      }
      
      // 1. 인증 및 STOMP 연결
      updateConnectionStep('auth')
      await authenticateAndConnect()
      
      // 2. STOMP 메시지 핸들러 설정
      setupStompMessageHandlers(client)
      
      // 3. 토론방 입장
      client.publish('/signaling/join', {
        type: 'join',
        roomId: targetRoomId,
        matchType: 1,
      })
      stepDone.auth.resolve()
      await stepDone.auth.promise
      console.log('🔄 인증 단계 완료')
      
      // 4. 라우터 정보 요청 및 Device 초기화
      updateConnectionStep('router')
      await initializeDevice()
      await stepDone.routerReady.promise
      console.log('🔄 라우터 정보 요청 단계 완료')
      
      // 5. recv 단계
      updateConnectionStep('recv')
      await createRecvTransport(client)
      // await connectRecvTransport(client, state.value.consumerTransport)
      await stepDone.recvReady.promise
      console.log('🔄 수신 준비 단계 완료')
      
      // 6. send 단계
      updateConnectionStep('send')
      await createProducerTransport(client)
      await connectProducerTransport(client, state.value.producerTransport)
      await stepDone.sendReady.promise
      console.log('🔄 송신 준비 단계 완료') 
      
      // 7. consumer 단계
      updateConnectionStep('consumer')
      await connectConsumers(client)
      await stepDone.consumerReady.promise
      console.log('🔄 다른 참가자와 연결 준비 단계 완료')
      
      // 연결 완료
      state.value.isConnecting = false
      state.value.isConnected = true
      
      console.log('✅ WebRTC 연결 프로세스 완료!')
      await stepDone.completed.promise
      return true
    } catch (error) {
      handleError('WebRTC 연결 실패', error)
      state.value.isConnecting = false
      state.value.isConnected = false
      throw error
    }
  }

  // 연결 해제
  const disconnectWebRTC = () => {
    console.log('🔌 WebRTC 연결 해제 시작')
    
    // 로컬 오디오 트랙 정리
    if (state.value.localAudioTrack) {
      state.value.localAudioTrack.stop()
      state.value.localAudioTrack = null
    }
    
    // 참가자들의 리소스 정리
    state.value.participants.forEach(participant => {
      // Consumer 정리
      if (participant.audioConsumer) {
        try {
          participant.audioConsumer.close()
        } catch (error) {
          console.warn('Audio Consumer 정리 중 오류:', error)
        }
      }
      
      // Consumer Transport 정리
      if (participant.consumerTransport) {
        try {
          participant.consumerTransport.close()
        } catch (error) {
          console.warn('Consumer Transport 정리 중 오류:', error)
        }
      }
      
      // Audio Stream 정리
      if (participant.audioStream) {
        participant.audioStream.getTracks().forEach((track: MediaStreamTrack) => {
          try {
            track.stop()
          } catch (error) {
            console.warn('Audio Track 정리 중 오류:', error)
          }
        })
      }
      
      participant.connected = false
      participant.audioStream = null
      participant.consumerTransport = null
      participant.audioConsumer = null
    })
    
    // Producer Transport 정리
    if (state.value.producerTransport) {
      state.value.producerTransport.close()
      state.value.producerTransport = undefined
    }
    
    // Device 정리
    if (state.value.device) {
      state.value.device = undefined
    }
    
    // 상태 초기화
    state.value.participants = []
    state.value.isConnecting = false
    state.value.isConnected = false
    state.value.connectionStep = 'auth'
    state.value.connectionStepText = connectionStepMessages.auth
    
    // 대기 중인 콜백 정리
    pendingTransportConnect.clear()
    pendingProducerCompleted.clear()
    
    // Producer ID 초기화
    produceA = null
    produceV = null
    
    // STOMP 연결 해제 (외부 클라이언트가 아닌 경우만)
    const client = getStompClient()
    if (client && !stompClient) {
      try {
        client.disconnect()
      } catch (error) {
        console.warn('STOMP 연결 해제 중 오류:', error)
      }
    }
    
    // Audio Controller 정리 (옵셔널)
    audioController?.cleanup()
    
    updateParticipants()
    console.log('✅ WebRTC 연결 해제 완료')
  }

  // 컴포넌트 언마운트 시 자동 정리
  onUnmounted(() => {
    disconnectWebRTC()
  })

  return {
    // 상태
    state,
    connectionError,
    roomId,
    allParticipantsConnected,
    connectionProgress,
    stepDone,

    // 사용자 정보
    getCurrentUser,
    config,
    
    // 연결 관리
    startWebRTCConnection,
    disconnectWebRTC,
    
    // 핸들러 함수들 (디버깅/테스트용)
    setupStompMessageHandlers,
    handleConsumerTransportCreated,
    handleConsumerCreated,
    handleNewProducer,
    
    // 의존성 접근자
    getStompClient,
    audioController,
  }
}