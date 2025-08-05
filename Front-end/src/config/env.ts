// 환경변수 설정
export const config = {
  // 로그인 서버 (OAuth, 인증)
  AUTH_API_URL: import.meta.env.VITE_AUTH_API_URL || 'http://localhost:8080',
  
  // 매칭 서버 (매칭, WebSocket)
  MATCH_API_URL: import.meta.env.VITE_MATCH_API_URL || 'http://localhost:8081',
  MATCH_WS_URL: import.meta.env.VITE_MATCH_WS_URL || 'ws://localhost:8081',
} 