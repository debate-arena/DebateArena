import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import type { WebSocketMessage, WebSocketMessageType } from '@/types/matching'
import { useAuthStore } from '@/store/auth'
import { config } from '@/config/env'

export const useWebSocket = () => {
  const stompClient = ref<Client | null>(null)
  const isConnected = ref(false)
  const authStore = useAuthStore()

  // WebSocket + STOMP 연결
  const connect = () => {
    return new Promise<void>((resolve, reject) => {
      const client = new Client({
        brokerURL: `${config.MATCH_WS_URL}/ws`,
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
    console.log('📤 매칭 요청 전송:', request)
  }

  // 매칭 수락/거절 전송
  const sendMatchAcceptance = (matchId: string, accept: boolean, team: number) => {
    if (!stompClient.value || !isConnected.value) {
      console.error('❌ STOMP 클라이언트가 연결되지 않음')
      return
    }

    stompClient.value.publish({
      destination: '/pub/match/acceptance',
      body: JSON.stringify({ matchId, accept, team })
    })
    console.log('📤 매칭 응답 전송:', { matchId, accept, team })
  }

  // 메시지 핸들러 제거
  const removeMessageHandler = () => {
    if (stompClient.value && isConnected.value) {
      // 모든 구독 해제
      stompClient.value.unsubscribe('/sub/match/status')
      stompClient.value.unsubscribe('/user/queue/match/personal')
      stompClient.value.unsubscribe('/user/queue/match/acceptance/status')
      stompClient.value.unsubscribe('/user/queue/error')
      console.log('🔌 WebSocket 메시지 핸들러 제거됨')
    }
  }

  // 메시지 수신 처리
  const handleMessage = (callback: (data: WebSocketMessage) => void) => {
    if (!stompClient.value) {
      console.error('❌ stompClient가 없음')
      return
    }
    
    if (!isConnected.value) {
      console.error('❌ WebSocket이 연결되지 않음')
      return
    }
    
    // 1. 매칭 현황판 구독 (전체 공지)
    stompClient.value.subscribe('/sub/match/status', (message) => {
      try {
        const data = JSON.parse(message.body)
        console.log('📥 매칭 현황판 수신:', data)
        callback({ type: 'MATCH_STATUS', status: data.status || 'success', data: data.data, message: data.message })
      } catch (error) {
        console.error('❌ 매칭 현황판 파싱 오류:', error)
      }
    })

    // 2. 개인 알림 구독 (매칭 초대장)
    stompClient.value.subscribe('/user/queue/match/acceptance', (message) => {
      try {
        const data = JSON.parse(message.body)
        console.log('📥 매칭 초대장 원본 메시지:', message.body)
        console.log('📥 매칭 초대장 파싱된 데이터:', data)
        
        // 이 경로는 매칭 초대장만 처리
        callback({ type: 'MATCH_INVITATION', status: data.status || 'success', data: data.data, message: data.message })
      } catch (error) {
        console.error('❌ 매칭 초대장 파싱 오류:', error)
      }
    })

    // 3. 다른 사람 응답 현황 구독 (실시간 피드백)
    stompClient.value.subscribe('/user/queue/match/acceptance/status', (message) => {
      try {
        console.log('📥 다른 사람 응답 현황 원본 메시지:', message.body)
        const parsedData = JSON.parse(message.body)
        console.log('📥 다른 사람 응답 현황 파싱된 데이터:', parsedData)
        
        // 올바른 데이터 구조로 접근
        const accept = parsedData.data?.accept
        const team = parsedData.data?.team || 0
        
        console.log('📥 다른 사람 응답 현황 추출된 값:', { accept, team })
        
        // 콜백에 올바른 데이터 구조로 전달
        callback({ 
          type: 'ACCEPTANCE_STATUS', 
          status: parsedData.status || 'success',
          data: {
            accept: accept,
            team: team
          },
          message: parsedData.message
        })
      } catch (error) {
        console.error('❌ 다른 사람 응답 현황 파싱 오류:', error)
      }
    })
    
    // 에러 구독
    stompClient.value.subscribe('/user/queue/error', (message) => {
      try {
        const data = JSON.parse(message.body)
        console.error('❌ 에러 수신:', data)
        callback({ type: 'ERROR', status: data.status || 'error', data: data.data, message: data.message })
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
    handleMessage,
    removeMessageHandler
  }
} 