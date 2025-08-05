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
  const sendMatchAcceptance = (matchId: string, accept: boolean, stance: number) => {
    if (!stompClient.value || !isConnected.value) {
      console.error('❌ STOMP 클라이언트가 연결되지 않음')
      return
    }

    stompClient.value.publish({
      destination: '/pub/match/acceptance',
      body: JSON.stringify({ matchId, accept, stance })
    })
    console.log('🎯 매칭 응답 전송:', { matchId, accept, stance })
  }

  // 메시지 수신 처리
  const handleMessage = (callback: (data: any) => void) => {
    console.log('🔍 handleMessage 호출됨')
    console.log('🔍 stompClient 상태:', !!stompClient.value)
    console.log('🔍 isConnected 상태:', isConnected.value)
    
    if (!stompClient.value) {
      console.error('❌ stompClient가 없음')
      return
    }
    
    if (!isConnected.value) {
      console.error('❌ WebSocket이 연결되지 않음')
      return
    }
    
    console.log('✅ 구독 설정 시작...')
    
    // 1. 매칭 현황판 구독 (전체 공지)
    console.log('🔍 매칭 현황판 구독 설정: /sub/match/status')
    stompClient.value.subscribe('/sub/match/status', (message) => {
      console.log('📨 매칭 현황판 메시지 수신됨!')
      try {
        const data = JSON.parse(message.body)
        console.log('📨 매칭 현황판 수신:', data)
        callback({ type: 'MATCH_STATUS', data })
      } catch (error) {
        console.error('❌ 매칭 현황판 파싱 오류:', error)
      }
    })

    // 2. 개인 알림 구독 (매칭 초대장 & 최종 결과)
    console.log('🔍 개인 알림 구독 설정: /user/queue/match/acceptance')
    stompClient.value.subscribe('/user/queue/match/acceptance', (message) => {
    
    // 추가: 다른 가능한 경로들도 구독
    console.log('🔍 추가 구독 설정: /user/queue/match')
    stompClient.value!.subscribe('/user/queue/match', (message) => {
      console.log('📨 추가 경로 메시지 수신됨!')
      console.log('🔍 추가 경로 메시지:', message)
      try {
        const data = JSON.parse(message.body)
        console.log('📨 추가 경로 파싱 완료:', data)
        callback({ type: 'MATCH_ADDITIONAL', data })
      } catch (error) {
        console.error('❌ 추가 경로 파싱 오류:', error)
      }
    })
    
    console.log('🔍 추가 구독 설정: /user/queue')
    stompClient.value!.subscribe('/user/queue', (message) => {
      console.log('📨 모든 개인 메시지 수신됨!')
      console.log('🔍 모든 개인 메시지:', message)
      try {
        const data = JSON.parse(message.body)
        console.log('📨 모든 개인 메시지 파싱 완료:', data)
        callback({ type: 'MATCH_ALL_PERSONAL', data })
      } catch (error) {
        console.error('❌ 모든 개인 메시지 파싱 오류:', error)
      }
    })
      try {
        console.log('📨 개인 알림 메시지 수신됨!')
        console.log('🔍 원본 메시지:', message)
        console.log('🔍 메시지 바디:', message.body)
        console.log('🔍 메시지 바디 타입:', typeof message.body)
        console.log('🔍 메시지 바디 길이:', message.body.length)
        console.log('🔍 메시지 전체 구조:', JSON.stringify(message, null, 2))
        
        const data = JSON.parse(message.body)
        console.log('📨 개인 알림 파싱 완료:', data)
        console.log('🔍 파싱된 데이터 타입:', typeof data)
        console.log('🔍 파싱된 데이터 키들:', Object.keys(data))
        console.log('🔍 status 값:', data.status)
        console.log('🔍 status 타입:', typeof data.status)
        console.log('🔍 data 값:', data.data)
        console.log('🔍 data 타입:', typeof data.data)
        console.log('🔍 전체 파싱된 데이터:', JSON.stringify(data, null, 2))
        
        // status에 따른 처리 (백엔드 ApiResponse 형태에 맞춤)
        console.log('🔍 status 값으로 분기 처리 시작:', data.status)
        switch (data.status) {
          case 'info':
            console.log('📨 매칭 초대장 수신:', data)
            console.log('🔍 매칭 초대장 data:', data.data)
            console.log('🔍 MATCH_INVITATION 콜백 호출 시도...')
            callback({ type: 'MATCH_INVITATION', data })
            console.log('🔍 MATCH_INVITATION 콜백 호출 완료')
            break
          case 'success':
            console.log('📨 매칭 성사 수신:', data)
            console.log('🔍 매칭 성사 data:', data.data)
            console.log('🔍 MATCH_SUCCESS 콜백 호출 시도...')
            callback({ type: 'MATCH_SUCCESS', data })
            console.log('🔍 MATCH_SUCCESS 콜백 호출 완료')
            break
          case 'warning':
            console.log('📨 매칭 실패 수신:', data)
            console.log('🔍 매칭 실패 data:', data.data)
            console.log('🔍 MATCH_FAILURE 콜백 호출 시도...')
            callback({ type: 'MATCH_FAILURE', data })
            console.log('🔍 MATCH_FAILURE 콜백 호출 완료')
            break
          case 'error':
            console.log('📨 매칭 에러 수신:', data)
            console.log('🔍 매칭 에러 data:', data.data)
            console.log('🔍 MATCH_ERROR 콜백 호출 시도...')
            callback({ type: 'MATCH_ERROR', data })
            console.log('🔍 MATCH_ERROR 콜백 호출 완료')
            break
          default:
            console.log('⚠️ 알 수 없는 개인 알림 타입:', data.status)
            console.log('🔍 전체 데이터:', data)
            console.log('🔍 status 값 (문자열):', JSON.stringify(data.status))
            console.log('🔍 status 값 (바이트):', Array.from(data.status || ''))
            console.log('🔍 MATCH_ACCEPTANCE 콜백 호출 시도...')
            callback({ type: 'MATCH_ACCEPTANCE', data })
            console.log('🔍 MATCH_ACCEPTANCE 콜백 호출 완료')
        }
        console.log('✅ 개인 알림 콜백 호출 완료')
      } catch (error) {
        console.error('❌ 개인 알림 메시지 파싱 오류:', error)
        console.error('🔍 원본 메시지:', message)
        console.error('🔍 원본 메시지 바디:', message.body)
      }
    })

    // 3. 다른 사람 응답 현황 구독 (실시간 피드백)
    console.log('🔍 응답 현황 구독 설정: /user/queue/match/acceptance/status')
    stompClient.value.subscribe('/user/queue/match/acceptance/status', (message) => {
      try {
        console.log('📨 다른 사람 응답 현황 수신됨!')
        console.log('🔍 원본 메시지:', message)
        console.log('🔍 메시지 바디:', message.body)
        const data = JSON.parse(message.body)
        console.log('📨 다른 사람 응답 현황 파싱 완료:', data)
        
        // 진영 정보가 포함된 응답 현황 처리
        const user = data.user
        const accept = data.accept
        const stance = data.stance || 'option2' // 기본값으로 option2 설정
        
        // 콜백에 진영 정보도 포함하여 전달
        callback({ 
          type: 'ACCEPTANCE_STATUS', 
          data: {
            ...data,
            stance: stance
          }
        })
        console.log('✅ 다른 사람 응답 현황 콜백 호출 완료 (진영 정보 포함)')
      } catch (error) {
        console.error('❌ 다른 사람 응답 현황 파싱 오류:', error)
        console.error('🔍 원본 메시지:', message)
      }
    })

    console.log('✅ 모든 구독 설정 완료')
    
    // 에러 구독
    console.log('🔍 에러 구독 설정: /user/queue/error')
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