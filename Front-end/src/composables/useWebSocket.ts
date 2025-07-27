import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 웹소켓 연결 관리 Composable
 * - 페이지 진입 시 자동 연결
 * - 연결 상태 관리
 * - 메시지 송수신 처리
 */
export function useWebSocket() {
  const socket = ref<WebSocket | null>(null)
  const isConnected = ref(false)
  const connectionError = ref<string | null>(null)

  // 웹소켓 연결
  const connectWebSocket = () => {
    // TODO: 웹소켓 연결 구현
    // - 서버 URL 설정 (예: ws://localhost:8000/ws)
    // - WebSocket 인스턴스 생성
    // - 연결 이벤트 핸들러 등록 (onopen, onclose, onerror, onmessage)
    // - 연결 상태 업데이트 (isConnected = true)
    console.log('🔌 웹소켓 연결 시도')
    
    // 가상 연결 성공 시뮬레이션
    setTimeout(() => {
      isConnected.value = true
      console.log('✅ 웹소켓 연결 성공')
    }, 1000)
  }

  // 웹소켓 메시지 처리
  const handleWebSocketMessage = (data: string) => {
    // TODO: 메시지 타입별 처리 구현
    // - JSON 파싱
    // - 메시지 타입 확인 (MATCH_FOUND, MATCH_TIMEOUT, QUEUE_UPDATE, ERROR 등)
    // - 타입별 적절한 핸들러 호출
    // - 매칭 성사 시: 매칭 정보 업데이트, 성사 모달 표시
    // - 타임아웃 시: 타임아웃 처리, 재시작 옵션 제공
    // - 대기열 업데이트 시: 대기열 상태 업데이트
    // - 에러 시: 에러 메시지 표시
    console.log('📨 웹소켓 메시지 수신:', data)
  }

  // 매칭 요청 전송
  const sendMatchingRequest = (selections: any) => {
    // TODO: 매칭 요청 전송 구현
    // - 선택된 주제/진영/모드 정보를 JSON으로 직렬화
    // - 서버에 매칭 요청 메시지 전송
    // - 전송 성공/실패 여부 반환
    console.log('📤 매칭 요청 전송:', selections)
    return true
  }

  // 매칭 취소 전송
  const sendMatchingCancel = () => {
    // TODO: 매칭 취소 전송 구현
    // - 서버에 매칭 취소 요청 메시지 전송
    // - 대기열에서 사용자 제거
    // - 전송 성공/실패 여부 반환
    console.log('❌ 매칭 취소 전송')
    return true
  }

  // 웹소켓 연결 해제
  const disconnectWebSocket = () => {
    // TODO: 웹소켓 연결 해제 구현
    // - WebSocket.close() 호출
    // - 연결 상태 초기화 (isConnected = false)
    // - 에러 상태 초기화
    console.log('🔌 웹소켓 연결 해제')
  }

  // 재연결 시도
  const reconnectWebSocket = () => {
    // TODO: 재연결 로직 구현
    // - 기존 연결 해제
    // - 일정 시간 후 재연결 시도 (지수 백오프)
    // - 최대 재시도 횟수 제한
    console.log('🔄 웹소켓 재연결 시도')
  }

  // 페이지 진입 시 자동 연결
  onMounted(() => {
    connectWebSocket()
  })

  // 페이지 이탈 시 연결 해제
  onUnmounted(() => {
    disconnectWebSocket()
  })

  return {
    isConnected,
    connectionError,
    sendMatchingRequest,
    sendMatchingCancel,
    reconnectWebSocket,
    disconnectWebSocket
  }
} 