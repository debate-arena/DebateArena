# 🧪 테스트 모드 설정 가이드

로그인 서버가 없는 상황에서 RTC 연결 테스트를 위한 설정 방법입니다.

## ⚙️ 환경 변수 설정

`Front-end` 폴더에 `.env` 파일을 생성하고 다음 내용을 추가하세요:

```env
# 백엔드 서버 URL
VITE_API_BASE_URL=http://70.12.246.177:8080
VITE_WS_BASE_URL=ws://70.12.246.177:8080

# 테스트 모드 활성화
VITE_TEST_MODE=true

# 테스트용 사용자 정보
VITE_TEST_USER_EMAIL=test@example.com
VITE_TEST_USER_NICKNAME=TestUser
```

## 🔧 테스트 모드 기능

테스트 모드가 활성화되면:

- ✅ **인증 우회**: 로그인 없이 STOMP 연결 가능
- ✅ **테스트 이메일 사용**: 설정한 테스트 이메일로 백엔드 연결
- ✅ **자동 연결**: 페이지 로드 시 자동으로 백엔드 연결 시도
- ✅ **디버그 로그**: 연결 상태와 메시지 송수신 상세 로그 출력

## 🚀 사용 방법

1. 위 환경 변수 설정 완료
2. 개발 서버 재시작: `npm run dev`
3. 토론방 페이지 접속
4. 브라우저 콘솔에서 연결 상태 확인:

```
🧪 테스트 모드: true | 테스트 이메일: test@example.com
🚀 토론방 초기화 시작
🔌 STOMP 연결 시작...
✅ STOMP 연결 성공
🔔 토론방 구독 시작...
✅ 토론방 구독 완료
```

## 🎯 테스트 가능한 기능

### STOMP 연결 테스트
- WebSocket 연결 상태 확인
- 메시지 송수신 테스트
- 재연결 기능 테스트

### WebRTC 시그널링 테스트
```javascript
// 테스트용 시그널링 메시지 송신
const { sendSignalingMessage } = useDebateConnection()
sendSignalingMessage('test-room', {
  type: 'offer',
  sdp: 'test-sdp-data'
})
```

### 토론 메시지 테스트
```javascript
// 테스트용 토론 메시지 송신
const { sendDebateMessage } = useDebateConnection()
sendDebateMessage('test-room', '안녕하세요!')
```

## 🛠️ 사용자 정보 확인

```javascript
const { getCurrentUser } = useDebateConnection()
const user = getCurrentUser()
console.log(user)
// { email: 'test@example.com', nickname: 'TestUser', isLoggedIn: true }
```

## ⚠️ 주의사항

- **프로덕션 배포 전**: `VITE_TEST_MODE=false`로 변경 필수
- **보안**: 실제 사용자 정보는 테스트 환경변수에 사용 금지
- **백엔드 연동**: 백엔드에서도 테스트 이메일을 허용하도록 설정 필요

## 🔄 정상 모드로 전환

로그인 서버 구축 완료 후:

```env
# .env 파일에서 테스트 모드 비활성화
VITE_TEST_MODE=false
```

그러면 기존 OAuth2 인증 시스템을 사용하게 됩니다. 