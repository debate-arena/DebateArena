import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from '@/store/auth'

export const useWebSocket = () => {
  const stompClient = ref<Client | null>(null)
  const isConnected = ref(false)
  const authStore = useAuthStore()

  // WebSocket + STOMP 연결
  const connect = () => {
    return new Promise<void>((resolve, reject) => {
      const client = new Client({
        brokerURL: 'wss://valid-grouse-randomly.ngrok-free.app/ws',
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000
      })

      // 인증 헤더 설정
      const headers = {
        'Authorization': `Bearer ${authStore.userEmail}` // 임시로 이메일 사용
      }

      client.onConnect = () => {
        console.log('🔗 WebSocket + STOMP 연결 성공')
        isConnected.value = true
        stompClient.value = client
        resolve()
      }

      client.onStompError = (error: any) => {
        console.error('❌ WebSocket + STOMP 연결 실패:', error)
        isConnected.value = false
        reject(error)
      }

      client.activate()
    })
  }

  // 연결 해제
  const disconnect = () => {
    if (stompClient.value && isConnected.value) {
      stompClient.value.deactivate()
      isConnected.value = false
      stompClient.value = null
      console.log('🔗 WebSocket + STOMP 연결 해제')
    }
  }

  // 매칭 요청 전송
  const sendMatchRequest = (request: any) => {
    if (!stompClient.value || !isConnected.value) {
      console.error('❌ STOMP 클라이언트가 연결되지 않음')
      return
    }

    stompClient.value.publish({
      destination: '/pub/match/request',
      body: JSON.stringify(request)
    })
    console.log('🎯 매칭 요청 전송:', request)
  }

  // 매칭 수락/거절 전송
  const sendMatchAcceptance = (matchId: string, accept: boolean) => {
    if (!stompClient.value || !isConnected.value) {
      console.error('❌ STOMP 클라이언트가 연결되지 않음')
      return
    }

    stompClient.value.publish({
      destination: '/pub/match/acceptance',
      body: JSON.stringify({ matchId, accept })
    })
    console.log('🎯 매칭 응답 전송:', { matchId, accept })
  }

  // 메시지 수신 처리
  const handleMessage = (callback: (data: any) => void) => {
    if (!stompClient.value) return
    
    // 1. 매칭 현황판 구독 (전체 공지)
    stompClient.value.subscribe('/sub/match/status', (message) => {
      try {
        const data = JSON.parse(message.body)
        console.log('📨 매칭 현황판 수신:', data)
        callback({ type: 'MATCH_STATUS', data })
      } catch (error) {
        console.error('❌ 매칭 현황판 파싱 오류:', error)
      }
    })

    // 2. 개인 알림 구독 (매칭 초대장 & 최종 결과)
    stompClient.value.subscribe('/user/queue/match/acceptance', (message) => {
      try {
        console.log('📨 개인 알림 메시지 수신됨!')
        console.log('🔍 원본 메시지:', message)
        console.log('🔍 메시지 바디:', message.body)
        console.log('🔍 메시지 바디 타입:', typeof message.body)
        console.log('🔍 메시지 바디 길이:', message.body.length)
        
        const data = JSON.parse(message.body)
        console.log('📨 개인 알림 파싱 완료:', data)
        console.log('🔍 파싱된 데이터 타입:', typeof data)
        console.log('🔍 파싱된 데이터 키들:', Object.keys(data))
        console.log('🔍 status 값:', data.status)
        console.log('🔍 status 타입:', typeof data.status)
        console.log('🔍 data 값:', data.data)
        console.log('🔍 data 타입:', typeof data.data)
        
        // status에 따른 처리 (백엔드 ApiResponse 형태에 맞춤)
        switch (data.status) {
          case 'info':
            console.log('📨 매칭 초대장 수신:', data)
            console.log('🔍 매칭 초대장 data:', data.data)
            callback({ type: 'MATCH_INVITATION', data })
            break
          case 'success':
            console.log('📨 매칭 성사 수신:', data)
            console.log('🔍 매칭 성사 data:', data.data)
            callback({ type: 'MATCH_SUCCESS', data })
            break
          case 'warning':
            console.log('📨 매칭 실패 수신:', data)
            console.log('🔍 매칭 실패 data:', data.data)
            callback({ type: 'MATCH_FAILURE', data })
            break
          case 'error':
            console.log('📨 매칭 에러 수신:', data)
            console.log('🔍 매칭 에러 data:', data.data)
            callback({ type: 'MATCH_ERROR', data })
            break
          default:
            console.log('⚠️ 알 수 없는 개인 알림 타입:', data.status)
            console.log('🔍 전체 데이터:', data)
            console.log('🔍 status 값 (문자열):', JSON.stringify(data.status))
            console.log('🔍 status 값 (바이트):', Array.from(data.status || ''))
            callback({ type: 'MATCH_ACCEPTANCE', data })
        }
        console.log('✅ 개인 알림 콜백 호출 완료')
      } catch (error) {
        console.error('❌ 개인 알림 메시지 파싱 오류:', error)
        console.error('🔍 원본 메시지:', message)
        console.error('🔍 원본 메시지 바디:', message.body)
      }
    })

    // 3. 다른 사람 응답 현황 구독 (실시간 피드백)
    stompClient.value.subscribe('/user/queue/match/acceptance/status', (message) => {
      try {
        console.log('📨 다른 사람 응답 현황 수신됨!')
        console.log('🔍 원본 메시지:', message)
        console.log('🔍 메시지 바디:', message.body)
        const data = JSON.parse(message.body)
        console.log('📨 다른 사람 응답 현황 파싱 완료:', data)
        callback({ type: 'ACCEPTANCE_STATUS', data })
        console.log('✅ 다른 사람 응답 현황 콜백 호출 완료')
      } catch (error) {
        console.error('❌ 다른 사람 응답 현황 파싱 오류:', error)
        console.error('🔍 원본 메시지:', message)
      }
    })

    // 에러 구독
    stompClient.value.subscribe('/user/queue/error', (message) => {
      try {
        const data = JSON.parse(message.body)
        console.error('❌ 에러 수신:', data)
        callback({ type: 'ERROR', data })
      } catch (error) {
        console.error('❌ 메시지 파싱 오류:', error)
      }
    })
  }

  // 컴포넌트 언마운트 시 연결 해제
  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    connect,
    disconnect,
    sendMatchRequest,
    sendMatchAcceptance,
    handleMessage
  }
} 