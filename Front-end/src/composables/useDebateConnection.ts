import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useStompConnection } from './useStompConnection'
import { useAuthStore } from '@/store/auth'

// WebRTC 관련 타입 정의
interface Participant {
  producerUserEmail: string
  producerId: string
  videoStream: MediaStream | null
  consumerTransport: any
}

// MediaSoup Device 타입 정의 (동적 로드)
declare global {
  interface Window {
    mediasoupClient: any
  }
}

/**
 * 토론방 STOMP 연결 관리 Composable
 * - 인증된 사용자의 STOMP 연결 자동 관리
 * - 토론방 메시지 송수신 처리
 */
export function useDebateConnection() {
  const authStore = useAuthStore()
  const { client, isConnected, connect, disconnect, subscribe, publish } = useStompConnection()
  
  const connectionError = ref<string | null>(null)
  const isConnecting = ref(false)
  
  // WebRTC 관련 상태
  const participants = ref<Participant[]>([])
  const isProducerTransportReady = ref(false)
  const localVideoRef = ref<HTMLVideoElement | null>(null)
  const isMediasoupLoaded = ref(false)
  
  // WebRTC 변수들
  let device: any = null
  let producerTransport: any = null
  let produceA: string | null = null
  let produceV: string | null = null
  
  // Transport 연결 콜백 관리
  const pendingTransportCallbacks = new Map<string, () => void>()
  // Producer 생성 콜백 관리  
  const pendingProducerCallbacks = new Map<string, (data: any) => void>()
  
  // 테스트 모드 설정 (로그인 서버가 없을 때 사용)
  const isTestMode = import.meta.env.VITE_TEST_MODE === 'true' || import.meta.env.NODE_ENV === 'development'
  const testUserEmail = import.meta.env.VITE_TEST_USER_EMAIL || 'test@example.com'
  const testUserNickname = import.meta.env.VITE_TEST_USER_NICKNAME || 'TestUser'
  
  console.log('🧪 테스트 모드:', isTestMode, '| 테스트 이메일:', testUserEmail)
  
  // 환경 변수에서 WebSocket URL 가져오기
  const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080'
  const signalingUrl = computed(() => `${WS_BASE_URL}/signaling`)
  
  // MediaSoup 클라이언트 로드
  const loadMediasoupClient = async (): Promise<boolean> => {
    try {
      // @ts-ignore - 동적 CDN 로드
      const mediasoupClient = await import('https://esm.sh/mediasoup-client@3')
      window.mediasoupClient = mediasoupClient
      isMediasoupLoaded.value = true
      console.log('✅ mediasoup-client 로드 성공:', mediasoupClient)
      return true
    } catch (error) {
      console.error('❌ mediasoup-client 로드 실패:', error)
      isMediasoupLoaded.value = false
      return false
    }
  }
  
  // 연결 시작
  const startConnection = async () => {
    // 테스트 모드가 아닌 경우에만 인증 체크
    if (!isTestMode && !authStore.isLoggedIn) {
      connectionError.value = '로그인이 필요합니다.'
      return false
    }
    
    if (isConnected.value) {
      console.log('이미 연결되어 있습니다.')
      return true
    }
    
    try {
      isConnecting.value = true
      connectionError.value = null
      
      // MediaSoup 클라이언트 로드
      const mediasoupLoaded = await loadMediasoupClient()
      if (!mediasoupLoaded) {
        throw new Error('MediaSoup 클라이언트 로드 실패')
      }
      
      // OAuth2 쿠키 기반 인증이므로 별도 토큰 요청 불필요
      // 기존 인증 상태를 활용하여 STOMP 연결 (테스트 모드일 때는 테스트 이메일 사용)
      const emailToUse = isTestMode ? testUserEmail : undefined

      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/auth/token`, {
        method: 'POST',
        mode: 'cors',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify({
          userEmail: emailToUse,
        }),
      });
      const data = await response.json();
      const authToken = data.token;

      connect(signalingUrl.value, authToken, emailToUse)
      
      // 연결 대기 (최대 5초)
      let attempts = 0
      while (!isConnected.value && attempts < 50) {
        await new Promise(resolve => setTimeout(resolve, 100))
        attempts++
      }
      
      if (!isConnected.value) {
        throw new Error('연결 시간 초과')
      }
      
      // STOMP 연결 성공 후 WebRTC 구독 설정
      setupWebRTCSubscriptions()
      
      return true
    } catch (error) {
      console.error('STOMP 연결 실패:', error)
      connectionError.value = error instanceof Error ? error.message : '연결에 실패했습니다.'
      return false
    } finally {
      isConnecting.value = false
    }
  }
  
  // WebRTC 구독 설정
  const setupWebRTCSubscriptions = () => {
    if (!isConnected.value) {
      console.error('❌ STOMP 연결이 필요합니다')
      return
    }
    
    // Router 정보 구독
    subscribe('/user/queue/router', (message) => {
      try {
        const routerInfo = JSON.parse(message.body)
        console.log('📡 Router 정보 수신:', routerInfo)
        handleRouterInfo(routerInfo)
      } catch (error) {
        console.error('❌ Router 정보 파싱 오류:', error)
      }
    })

    // Transport 정보 구독
    subscribe('/user/queue/transport', (message) => {
      try {
        const transportInfo = JSON.parse(message.body)
        console.log('🚛 Transport 정보 수신:', transportInfo)
        
        if (transportInfo.type === 'producerTransportCreated') {
          handleProducerTransportCreated(transportInfo)
        } else if (transportInfo.type === 'consumerTransportCreated') {
          handleConsumerTransportCreated(transportInfo)
        } else {
          console.warn('⚠️ 알 수 없는 Transport 타입:', transportInfo.type)
        }
      } catch (error) {
        console.error('❌ Transport 정보 파싱 오류:', error)
      }
    })

    // Transport 연결 응답 구독
    subscribe('/user/queue/transport-connected', (message) => {
      try {
        const connectInfo = JSON.parse(message.body)
        console.log('🔗 Transport 연결 응답 수신:', connectInfo)
        handleTransportConnected(connectInfo)
      } catch (error) {
        console.error('❌ Transport 연결 정보 파싱 오류:', error)
      }
    })

    // Producer 생성 응답 구독
    subscribe('/user/queue/producer', (message) => {
      try {
        const producerInfo = JSON.parse(message.body)
        console.log('📹 Producer 생성 응답 수신:', producerInfo)
        handleProducerCreated(producerInfo)
      } catch (error) {
        console.error('❌ Producer 정보 파싱 오류:', error)
      }
    })

    // Consumer 생성 응답 구독
    subscribe('/user/queue/consumer', (message) => {
      try {
        const consumerInfo = JSON.parse(message.body)
        console.log('🎬 Consumer 생성 응답 수신:', consumerInfo)
        handleConsumed(consumerInfo)
      } catch (error) {
        console.error('❌ Consumer 정보 파싱 오류:', error)
      }
    })

    // NewProducer 이벤트 구독
    subscribe('/user/queue/new-producer', (message) => {
      try {
        const newProducerInfo = JSON.parse(message.body)
        console.log('🆕 NewProducer 이벤트 수신:', newProducerInfo)
        handleNewProducer(newProducerInfo)
      } catch (error) {
        console.error('❌ NewProducer 정보 파싱 오류:', error)
      }
    })
    
    console.log('✅ WebRTC STOMP 구독 설정 완료')
  }
  
  // WebRTC 관련 핸들러 함수들
  
  // Router 정보 처리
  const handleRouterInfo = async (message: any) => {
    console.log('📡 Router 정보 수신:', message)
    
    if (!isMediasoupLoaded.value) {
      console.error('❌ mediasoup 클라이언트가 로드되지 않음')
      return
    }
    
    device = new window.mediasoupClient.Device()
    await device.load({ routerRtpCapabilities: message.rtpCapabilities })
    
    // 기존 참가자들 정보 저장
    if (message.participants && Array.isArray(message.participants)) {
      participants.value = message.participants.map((participant: any) => ({
        producerUserEmail: participant.producerUserEmail,
        producerId: participant.producerId,
        videoStream: null,
        consumerTransport: null
      }))
      console.log('👥 기존 참가자들 정보 저장:', participants.value)
      
      // 각 참가자에 대해 consumer transport 생성 요청
      participants.value.forEach(participant => {
        requestConsumerTransport(participant.producerUserEmail)
      })
    }
    
    // Producer transport 생성 요청
    requestProducerTransport()
  }
  
  // Producer Transport 요청
  const requestProducerTransport = () => {
    if (!isConnected.value) {
      console.error('❌ STOMP 연결이 필요합니다')
      return
    }
    
    const message = { isProducer: true }
    publish('/signaling/createTransport', message)
    console.log('🚀 Producer transport 생성 요청:', message)
  }
  
  // Consumer Transport 요청
  const requestConsumerTransport = (producerUserEmail: string) => {
    if (!isConnected.value) {
      console.error('❌ STOMP 연결이 필요합니다')
      return
    }
    
    const message = {
      producerUserEmail: producerUserEmail,
      isProducer: false
    }
    publish('/signaling/createTransport', message)
    console.log(`🔄 Consumer transport 생성 요청 (${producerUserEmail}):`, message)
  }
  
  // Transport 연결 응답 처리
  const handleTransportConnected = (connectInfo: any) => {
    const { connectedTransportId } = connectInfo
    const callback = pendingTransportCallbacks.get(connectedTransportId)
    
    if (callback) {
      console.log(`✅ Transport ${connectedTransportId} 연결 완료`)
      callback()
      pendingTransportCallbacks.delete(connectedTransportId)
    } else {
      console.warn(`⚠️ Transport ${connectedTransportId}에 대한 대기 중인 콜백이 없습니다`)
    }
  }
  
  // Producer 생성 응답 처리
  const handleProducerCreated = (producerInfo: any) => {
    const { producerId, kind } = producerInfo
    const callback = pendingProducerCallbacks.get(kind)
    
    if (callback) {
      console.log(`✅ Producer ${producerId} (${kind}) 생성 완료`)
      
      // Producer ID 저장
      if (kind === 'video') {
        produceV = producerId
      } else if (kind === 'audio') {
        produceA = producerId
      }
      
      callback({ id: producerId })
      pendingProducerCallbacks.delete(kind)
    } else {
      console.warn(`⚠️ Producer 요청 ${kind}에 대한 대기 중인 콜백이 없습니다`)
    }
  }
  
  // Producer Transport 생성 처리
  const handleProducerTransportCreated = async (message: any) => {
    console.log('🚛 Producer Transport 생성됨:', message.transportId)
    
    const { transportId, dtlsParameters, iceCandidates, iceParameters } = message
    
    producerTransport = device.createSendTransport({
      id: transportId,
      dtlsParameters,
      iceCandidates,
      iceParameters
    })
    
    producerTransport.on('connectionstatechange', (state: string) => {
      console.log('📡 Producer Transport 연결 상태:', state)
    })
    
    producerTransport.on('connect', ({ dtlsParameters }: any, callback: any, errback: any) => {
      // 대기 중인 콜백을 맵에 저장
      pendingTransportCallbacks.set(transportId, callback)
      
      if (!isConnected.value) {
        console.error('❌ STOMP 연결이 필요합니다')
        errback(new Error('STOMP not connected'))
        return
      }
      
      const connectMessage = {
        transportId: transportId,
        dtlsParameters: dtlsParameters
      }
      
      publish('/signaling/connectTransport', connectMessage)
      console.log('🔗 Producer Transport 연결 요청 전송:', connectMessage)
    })
    
    producerTransport.on('produce', ({ kind, rtpParameters }: any, callback: any, errback: any) => {
      if (!isConnected.value) {
        console.error('❌ STOMP 연결이 필요합니다')
        errback(new Error('STOMP not connected'))
        return
      }
      
      // 대기 중인 콜백을 맵에 저장
      pendingProducerCallbacks.set(kind, callback)
      
      const createProducerMessage = {
        transportId: transportId,
        kind: kind,
        rtpParameters: rtpParameters
      }
      
      publish('/signaling/createProducer', createProducerMessage)
      console.log('📹 Producer 생성 요청 전송:', createProducerMessage)
    })

    isProducerTransportReady.value = true
    console.log('✅ Producer transport 준비 완료')
  }
  
  // Consumer Transport 생성 처리
  const handleConsumerTransportCreated = async (message: any) => {
    const { transportId, dtlsParameters, iceCandidates, iceParameters, producerUserEmail } = message
    
    // 해당 참가자 찾기
    const participant = participants.value.find(p => p.producerUserEmail === producerUserEmail)
    if (!participant) {
      console.error(`❌ 참가자 ${producerUserEmail}를 찾을 수 없습니다`)
      return
    }
    
    // Consumer transport 생성
    const transport = device.createRecvTransport({
      id: transportId,
      dtlsParameters,
      iceCandidates,
      iceParameters
    })

    // 참가자의 consumerTransport 필드에 저장
    participant.consumerTransport = transport

    transport.on('connect', ({ dtlsParameters }: any, callback: any, errback: any) => {
      // 대기 중인 콜백을 맵에 저장
      pendingTransportCallbacks.set(transportId, () => {
        console.log(`✅ 참가자 ${producerUserEmail}의 consumer transport 연결됨`)
        callback()
      })
      
      if (!isConnected.value) {
        console.error('❌ STOMP 연결이 필요합니다')
        errback(new Error('STOMP not connected'))
        return
      }
      
      const connectMessage = {
        type: 'connectTransport',
        transportId: transportId,
        dtlsParameters: dtlsParameters
      }
      
      publish('/signaling/connectTransport', connectMessage)
      console.log('🔗 Consumer Transport 연결 요청 전송:', connectMessage)
    })
    
    console.log(`🔄 참가자 ${producerUserEmail}의 consumer transport 생성됨`)
    createConsumerForParticipant(participant)
  }
  
  // 참가자의 consumer 생성
  const createConsumerForParticipant = (participant: Participant) => {
    if (!isConnected.value) {
      console.error('❌ STOMP 연결이 필요합니다')
      return
    }
    
    const createConsumerMessage = {
      transportId: participant.consumerTransport.id,
      producerId: participant.producerId,
      producerUserEmail: participant.producerUserEmail,
      rtpCapabilities: device.rtpCapabilities
    }
    
    publish('/signaling/createConsumer', createConsumerMessage)
    console.log(`🎯 참가자 ${participant.producerUserEmail}의 consumer 생성 요청:`, createConsumerMessage)
  }
  
  // Consumer 생성 완료 처리
  const handleConsumed = async (message: any) => {
    console.log('🎬 Consumer 생성됨:', message)
    const { consumerId, rtpParameters, kind, producerUserEmail, producerId } = message
    
    // 해당 참가자 찾기
    const participant = participants.value.find(p => p.producerUserEmail === producerUserEmail)
    if (!participant) {
      console.error(`❌ 참가자 ${producerUserEmail}를 찾을 수 없습니다`)
      return
    }
    
    const consumer = await participant.consumerTransport.consume({
      id: consumerId,
      producerId: producerId,
      kind,
      rtpParameters
    })

    const { track } = consumer
    const stream = new MediaStream([track])
    
    // 참가자의 videoStream 필드에 저장
    if (kind === 'video') {
      participant.videoStream = stream
      console.log(`📹 참가자 ${producerUserEmail}의 비디오 스트림 설정됨`)
    }
    
    // Consumer resume 요청
    if (!isConnected.value) {
      console.error('❌ STOMP 연결이 필요합니다')
      return
    }
    
    const resumeMessage = {
      type: 'resume',
      consumerId: consumerId
    }
    
    publish('/signaling/resume', resumeMessage)
    console.log('▶️ Consumer resume 요청 전송:', resumeMessage)
  }
  
  // 새로운 Producer 처리
  const handleNewProducer = async (message: any) => {
    console.log('🆕 새로운 Producer 이벤트 받음:', message)
    
    const { producerUserEmail, producerId } = message
    
    // 기존 participants 배열에 이미 있는 사용자인지 확인
    const existingParticipant = participants.value.find(p => p.producerUserEmail === producerUserEmail)
    if (existingParticipant) {
      console.log(`👤 참가자 ${producerUserEmail}는 이미 존재하므로 무시합니다`)
      return
    }
    
    console.log(`➕ 새로운 참가자 ${producerUserEmail} 추가 시작`)
    
    // participants 배열에 새로운 참가자 추가
    const newParticipant: Participant = {
      producerUserEmail: producerUserEmail,
      producerId: producerId,
      videoStream: null,
      consumerTransport: null
    }
    
    participants.value.push(newParticipant)
    console.log('👥 참가자 배열 업데이트됨:', participants.value)
    
    // 새로운 참가자를 위한 consumer transport 생성 요청
    requestConsumerTransport(producerUserEmail)
  }
  
  // 미디어 스트림 시작 (로컬 비디오/오디오)
  const startProduce = async (): Promise<boolean> => {
    if (!isProducerTransportReady.value) {
      console.error('❌ Producer transport가 준비되지 않았습니다')
      return false
    }
    
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true
      })
      
      // 로컬 비디오 요소에 스트림 설정
      if (localVideoRef.value) {
        localVideoRef.value.srcObject = stream
      }
      
      const videoTrack = stream.getVideoTracks()[0]
      if (videoTrack) {
        await producerTransport.produce({
          track: videoTrack,
          codecOptions: {
            videoGoogleStartBitrate: 1000
          }
        })
      }
      
      const audioTrack = stream.getAudioTracks()[0]
      if (audioTrack) {
        await producerTransport.produce({
          track: audioTrack
        })
      }
      
      console.log('🎥 미디어 스트림 시작 완료')
      return true
      
    } catch (error) {
      console.error('❌ 미디어 접근 오류:', error)
      return false
    }
  }
  
  // 토론방 입장 (WebRTC 지원)
  const joinRoom = (roomId: string) => {
    if (!isConnected.value) {
      console.warn('❌ STOMP 연결이 필요합니다.')
      return null
    }

    if (!isMediasoupLoaded.value) {
      console.warn('❌ MediaSoup 클라이언트가 로드되지 않았습니다.')
      return null
    }

    const joinMessage = {
      type: 'join',
      roomId: roomId,
      matchType: 1,
    }
    
    publish('/signaling/join', joinMessage)
    console.log('🚪 토론방 입장 메시지 전송:', joinMessage)

    // 방별 메시지 구독 (채팅 등)
    const roomSubscription = subscribe(`/topic/room/${roomId}`, (message) => {
      const body = JSON.parse(message.body)
      console.log('💬 토론방 메시지 수신:', body)
      // TODO: 메시지 타입별 처리 로직 구현
    })
    
    return roomSubscription
  }
  
  // 토론방 퇴장 (리소스 정리)
  const leaveRoom = () => {
    console.log('🚪 토론방 퇴장 시작')
    
    isProducerTransportReady.value = false
    
    // 로컬 스트림 정리
    if (localVideoRef.value && localVideoRef.value.srcObject) {
      const stream = localVideoRef.value.srcObject as MediaStream
      stream.getTracks().forEach(track => {
        track.stop()
        console.log(`🛑 로컬 트랙 정지: ${track.kind}`)
      })
      localVideoRef.value.srcObject = null
    }
    
    // 참가자들의 transport 정리
    participants.value.forEach(participant => {
      if (participant.consumerTransport) {
        participant.consumerTransport.close()
        console.log(`🗑️ ${participant.producerUserEmail}의 consumer transport 정리`)
      }
      if (participant.videoStream) {
        participant.videoStream.getTracks().forEach(track => track.stop())
      }
    })
    
    // 참가자 배열 초기화
    participants.value = []
    
    // Producer Transport 정리
    if (producerTransport) {
      producerTransport.close()
      producerTransport = null
      console.log('🗑️ Producer transport 정리')
    }
    
    // Device 정리
    if (device) {
      device = null
      console.log('🗑️ MediaSoup device 정리')
    }
    
    // Producer ID 초기화
    produceA = null
    produceV = null
    
    // 대기 중인 콜백 정리
    pendingTransportCallbacks.clear()
    pendingProducerCallbacks.clear()
    
    console.log('✅ 토론방 퇴장 완료')
  }
  
  // WebRTC 시그널링 메시지 송신
  const sendSignalingMessage = (roomId: string, message: any) => {
    if (!isConnected.value) {
      console.warn('STOMP 연결이 필요합니다.')
      return
    }
    
    const senderEmail = isTestMode ? testUserEmail : authStore.userEmail
    
    publish(`/app/room/${roomId}/signal`, {
      sender: senderEmail,
      ...message
    })
  }
  
  // 토론 메시지 송신
  const sendDebateMessage = (roomId: string, message: string) => {
    if (!isConnected.value) {
      console.warn('STOMP 연결이 필요합니다.')
      return
    }
    
    const senderEmail = isTestMode ? testUserEmail : authStore.userEmail
    const senderNickname = isTestMode ? testUserNickname : authStore.userNickname
    
    publish(`/app/room/${roomId}/message`, {
      sender: senderEmail,
      nickname: senderNickname,
      message,
      timestamp: new Date().toISOString()
    })
  }
  
  // 연결 종료 (모든 리소스 정리)
  const stopConnection = () => {
    // 토론방 리소스 정리
    leaveRoom()
    
    // STOMP 연결 종료
    disconnect()
    connectionError.value = null
    
    // MediaSoup 상태 초기화
    isMediasoupLoaded.value = false
    
    console.log('🔌 연결 완전 종료')
  }

  // 현재 사용자 정보 (테스트 모드 지원)
  const getCurrentUser = () => {
    return {
      email: isTestMode ? testUserEmail : authStore.userEmail,
      nickname: isTestMode ? testUserNickname : authStore.userNickname,
      isLoggedIn: isTestMode ? true : authStore.isLoggedIn
    }
  }
  
  // 자동 연결 (테스트 모드이거나 인증된 사용자만)
  onMounted(() => {
    if (isTestMode || authStore.isLoggedIn) {
      startConnection()
    }
  })
  
  // 자동 연결 해제
  onUnmounted(() => {
    stopConnection()
  })
  
  return {
    // 기본 연결 상태
    isConnected,
    isConnecting,
    connectionError,
    isTestMode,
    
    // WebRTC 상태
    participants,
    isProducerTransportReady,
    isMediasoupLoaded,
    localVideoRef,
    
    // 사용자 정보
    getCurrentUser,
    
    // 기본 연결 액션
    startConnection,
    stopConnection,
    joinRoom,
    leaveRoom,
    sendSignalingMessage,
    sendDebateMessage,
    
    // WebRTC 액션
    startProduce,
    
    // STOMP 클라이언트 직접 접근 (고급 사용)
    client
  }
} 