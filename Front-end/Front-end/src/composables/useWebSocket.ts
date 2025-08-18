import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import type { WebSocketMessage } from '@/types/matching'
import { config } from '@/config/env'

export const useWebSocket = () => {
  const stompClient = ref<Client | null>(null)
  const isConnected = ref(false)
  // 연결 중복 방지용 in-flight Promise와 구독 핸들 보관
  let connectInFlight: Promise<void> | null = null
  let isSubscribed = false
  let subscriptions: Array<{ unsubscribe: () => void }> = []

  // WebSocket + STOMP 연결
  const connect = () => {
    if (isConnected.value) {
      return Promise.resolve()
    }
    if (connectInFlight) {
      return connectInFlight
    }
    connectInFlight = new Promise<void>((resolve, reject) => {
      const client = new Client({
        brokerURL: `${config.MATCH_WS_URL}/ws`,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
      })

      client.onConnect = () => {
        console.log('🔗 WebSocket 연결 성공')
        isConnected.value = true
        stompClient.value = client
        connectInFlight = null
        resolve()
      }

      client.onStompError = (error: any) => {
        console.error('❌ WebSocket STOMP 에러:', error)
        isConnected.value = false
        stompClient.value = null
        // 구독 상태 초기화
        isSubscribed = false
        subscriptions = []
        connectInFlight = null
        reject(error)
      }

      client.onWebSocketClose = () => {
        console.warn('🔌 WebSocket 연결 종료')
        isConnected.value = false
        stompClient.value = null
        // 구독 상태 초기화
        isSubscribed = false
        subscriptions = []
      }

      client.activate()
    })
    return connectInFlight
  }

  // 연결 해제
  const disconnect = () => {
    if (stompClient.value && isConnected.value) {
      try {
        // 저장된 구독 해제
        for (const sub of subscriptions) {
          try { sub.unsubscribe() } catch {}
        }
        subscriptions = []
        isSubscribed = false
        stompClient.value.deactivate()
      } finally {
        isConnected.value = false
        stompClient.value = null
      }
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
  }

  // 메시지 핸들러 제거
  const removeMessageHandler = () => {
    if (stompClient.value && isConnected.value) {
      for (const sub of subscriptions) {
        try { sub.unsubscribe() } catch {}
      }
      subscriptions = []
      isSubscribed = false
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

    if (isSubscribed) {
      // 이미 구독되어 있음
      return
    }
    
    // 1. 매칭 현황판 구독 (전체 공지)
    const sub1 = stompClient.value.subscribe('/sub/match/status', (message) => {
      try {
        const data = JSON.parse(message.body)
        callback({ type: 'MATCH_STATUS', status: data.status || 'success', data: data.data, message: data.message })
      } catch (error) {
        console.error('❌ 매칭 현황판 파싱 오류:', error)
      }
    })

    // 2. 개인 알림 구독 (매칭 초대장)
    const sub2 = stompClient.value.subscribe('/user/queue/match/acceptance', (message) => {
      try {
        const data = JSON.parse(message.body)
        callback({ type: 'MATCH_INVITATION', status: data.status || 'success', data: data.data, message: data.message })
      } catch (error) {
        console.error('❌ 매칭 초대장 파싱 오류:', error)
      }
    })

    // 3. 다른 사람 응답 현황 구독 (실시간 피드백)
    const sub3 = stompClient.value.subscribe('/user/queue/match/acceptance/status', (message) => {
      try {
        const parsedData = JSON.parse(message.body)
        
        // 올바른 데이터 구조로 접근
        const accept = parsedData.data?.accept
        const team = parsedData.data?.team || 0
        
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

    // 4. 매칭 결과 구독 (새로 추가)
    const sub4 = stompClient.value.subscribe('/user/queue/match/acceptance/result', (message) => {
      console.log('🎯 /user/queue/match/acceptance/result 구독으로 메시지 수신됨!')
      console.log('📥 매칭 결과 원본 메시지:', message.body)
      
      try {
        const data = JSON.parse(message.body)
        console.log('📥 매칭 결과 파싱된 데이터:', data)
        console.log('📥 매칭 결과 데이터 구조:', {
          status: data.status,
          data: data.data,
          message: data.message,
          hasRoomId: !!(data.data?.roomId || data.roomId)
        })
        
        // 매칭 결과 데이터 전달
        callback({ 
          type: 'MATCH_RESULT', 
          status: data.status || 'success', 
          data: data.data, 
          message: data.message 
        })
      } catch (error) {
        console.error('❌ 매칭 결과 파싱 오류:', error)
        console.error('❌ 파싱 실패한 원본 메시지:', message.body)
      }
    })
    
    // 에러 구독
    const sub5 = stompClient.value.subscribe('/user/queue/error', (message) => {
      try {
        const data = JSON.parse(message.body)
        console.error('❌ 에러 수신:', data)
        callback({ type: 'ERROR', status: data.status || 'error', data: data.data, message: data.message })
      } catch (error) {
        console.error('❌ 메시지 파싱 오류:', error)
      }
    })

    subscriptions = [sub1, sub2, sub3, sub4, sub5]
    isSubscribed = true
  }

  // 컴포넌트 언마운트 시 연결 해제
  onUnmounted(() => {
    disconnect()
  })

  return {
    stompClient,
    isConnected,
    connect,
    disconnect,
    sendMatchRequest,
    sendMatchAcceptance,
    handleMessage,
    removeMessageHandler
  }
} 