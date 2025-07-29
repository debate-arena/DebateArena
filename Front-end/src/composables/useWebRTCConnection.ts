import { ref, computed } from 'vue'
import { Device } from 'mediasoup-client'

export interface WebRTCParticipant {
  id: number
  name: string
  email: string
  profileImage: string
  connected: boolean
  producerTransport?: any
  consumerTransport?: any
  audioProducer?: any
  videoProducer?: any
  audioConsumer?: any
  videoConsumer?: any
}

export interface WebRTCConnectionState {
  isConnecting: boolean
  isConnected: boolean
  connectionStep: 'router' | 'transport' | 'producer' | 'consumer' | 'completed'
  connectionStepText: string
  participants: WebRTCParticipant[]
  device?: Device
  routerRtpCapabilities?: any
}

export const useWebRTCConnection = () => {
  const state = ref<WebRTCConnectionState>({
    isConnecting: false,
    isConnected: false,
    connectionStep: 'router',
    connectionStepText: '라우터 정보 요청 중...',
    participants: [],
    device: undefined,
    routerRtpCapabilities: undefined
  })

  const roomId = ref<number>(1) // 실제로는 라우터에서 받아와야 함
  const currentUserId = ref<number>(1) // 실제로는 로그인 정보에서 받아와야 함

  // 연결 단계별 메시지
  const connectionStepMessages = {
    router: '라우터 정보 요청 중...',
    transport: 'Transport 생성 중...',
    producer: 'Producer 생성 중...',
    consumer: 'Consumer 생성 중...',
    completed: '연결 완료!'
  }

  // 모든 참가자가 연결되었는지 확인
  const allParticipantsConnected = computed(() => {
    return state.value.participants.length > 0 && 
           state.value.participants.every(p => p.connected)
  })

  // 서버 API 호출 함수들 (테스트용 mock)
  const apiCall = async (endpoint: string, data: any) => {
    try {
      // 실제 서버 통신 대신 mock 데이터 반환
      console.log(`📡 API 호출: ${endpoint}`, data)
      
      // 연결 과정을 시뮬레이션하기 위한 지연
      await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 2000))
      
      switch (endpoint) {
        case 'router':
          return {
            rtpCapabilities: {
              codecs: [
                {
                  mimeType: 'audio/opus',
                  clockRate: 48000,
                  channels: 2
                }
              ],
              headerExtensions: [],
              fecMechanisms: []
            }
          }
          
        case 'transport/create':
          return {
            id: `transport-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
            iceParameters: {
              usernameFragment: 'mock-username',
              password: 'mock-password'
            },
            iceCandidates: [
              {
                foundation: 'mock-foundation',
                priority: 1234567890,
                ip: '127.0.0.1',
                port: 12345,
                type: 'host',
                protocol: 'udp'
              }
            ],
            dtlsParameters: {
              role: 'auto',
              fingerprints: [
                {
                  algorithm: 'sha-256',
                  value: 'mock-fingerprint'
                }
              ]
            }
          }
          
        case 'transport/connect':
          return { success: true }
          
        case 'producer/create':
          return {
            id: `producer-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
          }
          
        case 'consumer/create':
          return {
            id: `consumer-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
            producerId: data.producerId,
            kind: 'audio',
            rtpParameters: {
              codecs: [
                {
                  mimeType: 'audio/opus',
                  clockRate: 48000,
                  channels: 2,
                  payloadType: 111
                }
              ],
              headerExtensions: [],
              encodings: [{ ssrc: 123456789 }]
            }
          }
          
        default:
          throw new Error(`Unknown endpoint: ${endpoint}`)
      }
    } catch (error) {
      console.error(`API call to ${endpoint} failed:`, error)
      throw error
    }
  }

  // 1. 라우터 정보 요청
  const requestRouterInfo = async () => {
    console.log('🔄 라우터 정보 요청 중...')
    state.value.connectionStep = 'router'
    state.value.connectionStepText = connectionStepMessages.router

    try {
      const response = await apiCall('router', {
        roomId: roomId.value
      })

      state.value.routerRtpCapabilities = response.rtpCapabilities

      // mediasoup Device 초기화
      state.value.device = new Device()
      await state.value.device.load({ routerRtpCapabilities: response.rtpCapabilities as any })

      console.log('✅ 라우터 정보 수신 완료')
      return response
    } catch (error) {
      console.error('❌ 라우터 정보 요청 실패:', error)
      throw error
    }
  }

  // 2. Transport 생성
  const createTransports = async () => {
    console.log('🔄 Transport 생성 중...')
    state.value.connectionStep = 'transport'
    state.value.connectionStepText = connectionStepMessages.transport

    try {
      // Producer Transport 생성
      const producerTransportData = await apiCall('transport/create', {
        isProducer: true
      })

      // Consumer Transport 생성 (다른 참가자들에 대해)
      const consumerTransportData = await apiCall('transport/create', {
        producerUserEmail: "test@example.com", // 실제로는 각 참가자의 이메일
        isProducer: false
      })

      // Transport 객체 생성
      const producerTransport = state.value.device!.createSendTransport({
        id: producerTransportData.id,
        iceParameters: producerTransportData.iceParameters,
        iceCandidates: producerTransportData.iceCandidates,
        dtlsParameters: producerTransportData.dtlsParameters
      } as any)

      const consumerTransport = state.value.device!.createRecvTransport({
        id: consumerTransportData.id,
        iceParameters: consumerTransportData.iceParameters,
        iceCandidates: consumerTransportData.iceCandidates,
        dtlsParameters: consumerTransportData.dtlsParameters
      } as any)

      // Transport 연결 이벤트 처리
      producerTransport.on('connect', async ({ dtlsParameters }, callback, errback) => {
        try {
          await apiCall('transport/connect', {
            transportId: producerTransport.id,
            dtlsParameters
          })
          callback()
        } catch (error) {
          errback(error as Error)
        }
      })

      consumerTransport.on('connect', async ({ dtlsParameters }, callback, errback) => {
        try {
          await apiCall('transport/connect', {
            transportId: consumerTransport.id,
            dtlsParameters
          })
          callback()
        } catch (error) {
          errback(error as Error)
        }
      })

      // Producer Transport의 produce 이벤트 처리
      producerTransport.on('produce', async ({ kind, rtpParameters }, callback, errback) => {
        try {
          const response = await apiCall('producer/create', {
            transportId: producerTransport.id,
            kind,
            rtpParameters
          })
          callback({ id: response.id! })
        } catch (error) {
          errback(error as Error)
        }
      })

      console.log('✅ Transport 생성 완료')
      return { producerTransport, consumerTransport }
    } catch (error) {
      console.error('❌ Transport 생성 실패:', error)
      throw error
    }
  }

  // 3. Producer 생성 (음성 스트림)
  const createProducer = async (transport: any) => {
    console.log('🔄 Producer 생성 중...')
    state.value.connectionStep = 'producer'
    state.value.connectionStepText = connectionStepMessages.producer

    try {
      // 사용자 미디어 스트림 요청
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: false // 음성만 사용
      })

      const audioTrack = stream.getAudioTracks()[0]
      
      // Producer 생성
      const producer = await transport.produce({
        track: audioTrack,
        codec: state.value.device!.rtpCapabilities.codecs?.find(
          codec => codec.mimeType.toLowerCase() === 'audio/opus'
        )
      })

      console.log('✅ Producer 생성 완료:', producer.id)
      return producer
    } catch (error) {
      console.error('❌ Producer 생성 실패:', error)
      throw error
    }
  }

  // 4. Consumer 생성
  const createConsumer = async (transport: any, producerId: string, producerUserEmail: string) => {
    console.log('🔄 Consumer 생성 중...')
    state.value.connectionStep = 'consumer'
    state.value.connectionStepText = connectionStepMessages.consumer

    try {
      const response = await apiCall('consumer/create', {
        transportId: transport.id,
        producerId,
        producerUserEmail,
        rtpCapabilities: state.value.device!.rtpCapabilities
      })

      const consumer = await transport.consume({
        id: response.id,
        producerId: response.producerId,
        kind: response.kind,
        rtpParameters: response.rtpParameters
      } as any)

      console.log('✅ Consumer 생성 완료:', consumer.id)
      return consumer
    } catch (error) {
      console.error('❌ Consumer 생성 실패:', error)
      throw error
    }
  }

  // 참가자 초기화
  const initializeParticipants = (participants: any[]) => {
    state.value.participants = participants.map((p: any) => ({
      id: p.id,
      name: p.name,
      email: p.email,
      profileImage: p.profileImage,
      connected: false
    }))
  }

  // 전체 WebRTC 연결 프로세스
  const startWebRTCConnection = async (participants: any[]) => {
    try {
      state.value.isConnecting = true
      state.value.isConnected = false

      // 참가자 초기화
      initializeParticipants(participants)

      // 1. 라우터 정보 요청
      await requestRouterInfo()

      // 2. Transport 생성
      const { producerTransport, consumerTransport } = await createTransports()

      // 3. Producer 생성 (자신의 오디오)
      const producer = await createProducer(producerTransport)

      // 4. 다른 참가자들의 Consumer 생성 (실제로는 서버에서 참가자 목록을 받아와야 함)
      const consumers: any[] = []
      for (const participant of state.value.participants) {
        if (participant.id !== currentUserId.value) {
          // 실제로는 해당 참가자의 Producer ID를 서버에서 받아와야 함
          try {
            // 테스트용: 점진적으로 참가자 연결 시뮬레이션
            await new Promise(resolve => setTimeout(resolve, 500))
            
            const consumer = await createConsumer(consumerTransport, 'dummy-producer-id', participant.email)
            consumers.push(consumer)
            participant.connected = true
            
            console.log(`✅ 참가자 ${participant.name} 연결 완료`)
          } catch (error) {
            console.warn(`참가자 ${participant.name}의 Consumer 생성 실패:`, error)
            // 테스트용: 실패해도 연결된 것으로 처리
            participant.connected = true
          }
        } else {
          // 자신은 Producer를 생성했으므로 연결된 것으로 처리
          participant.connected = true
        }
      }

      // 연결 완료
      state.value.connectionStep = 'completed'
      state.value.connectionStepText = connectionStepMessages.completed
      state.value.isConnecting = false
      state.value.isConnected = true

      console.log('✅ WebRTC 연결 완료!')

      return {
        producerTransport,
        consumerTransport,
        producer,
        consumers
      }
    } catch (error) {
      console.error('❌ WebRTC 연결 실패:', error)
      state.value.isConnecting = false
      state.value.isConnected = false
      throw error
    }
  }

  // 연결 해제
  const disconnectWebRTC = () => {
    state.value.isConnecting = false
    state.value.isConnected = false
    state.value.connectionStep = 'router'
    state.value.connectionStepText = connectionStepMessages.router
    state.value.participants.forEach(p => p.connected = false)
    
    console.log('🔌 WebRTC 연결 해제')
  }

  return {
    state: state.value,
    roomId,
    currentUserId,
    allParticipantsConnected,
    startWebRTCConnection,
    disconnectWebRTC
  }
} 