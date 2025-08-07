// 환경변수 설정
export const config = {
  // 메인 서버 (OAuth, 인증, 주제)
  MAIN_API_URL: import.meta.env.VITE_MAIN_API_URL || 'http://localhost:8080',
  
  // 로그인 서버 (OAuth, 인증) - 기존 호환성 유지
  AUTH_API_URL: import.meta.env.VITE_AUTH_API_URL || 'http://localhost:8080',
  
  // 매칭 서버 (매칭, WebSocket)
  MATCH_API_URL: import.meta.env.VITE_MATCH_API_URL || 'http://localhost:8081',
  MATCH_WS_URL: import.meta.env.VITE_MATCH_WS_URL || 'ws://localhost:8081',
  
  // STT 서버 (음성 인식, WebSocket)
  STT_API_URL: import.meta.env.VITE_STT_API_URL || 'http://localhost:8082',
  STT_WS_URL: import.meta.env.VITE_STT_WS_URL || 'ws://localhost:8082',
} 