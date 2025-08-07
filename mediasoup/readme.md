# MediaSoup WebRTC SFU Server

MediaSoup 기반의 WebRTC SFU(Selective Forwarding Unit) 서버입니다. Redis Pub/Sub을 통한 분산 환경을 지원합니다.

## 🛠️ 기술 스택

- **Node.js** (>=16.0.0)
- **MediaSoup** - WebRTC 미디어 서버
- **Redis** - Pub/Sub 통신
- **UUID** - 고유 ID 생성

## 📋 요구사항

- Node.js 16 이상
- Redis 서버
- 방화벽에서 UDP 포트 범위 허용 (기본: 40000-40100)

## 🚀 설치 및 실행

### 1. 의존성 설치
```bash
npm install
```

### 2. 환경변수 설정
`.env.production` 파일을 생성하고 다음 내용을 설정하세요:

```env
# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# MediaSoup Configuration
MEDIASOUP_ANNOUNCED_IP=70.12.246.177  # 공인 IP 주소로 변경
MEDIASOUP_LISTEN_IP=0.0.0.0
MEDIASOUP_PORT_RANGE_MIN=40000
MEDIASOUP_PORT_RANGE_MAX=40100
MEDIASOUP_LOG_LEVEL=warn
```

### 3. 서버 실행
```bash
# 프로덕션 실행
npm start

# 개발환경 실행 (nodemon 사용)
npm run dev
```

## ⚙️ 환경변수 설명

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `REDIS_HOST` | Redis 서버 호스트 | localhost |
| `REDIS_PORT` | Redis 서버 포트 | 6379 |
| `REDIS_PASSWORD` | Redis 비밀번호 | - |
| `MEDIASOUP_ANNOUNCED_IP` | 외부 접근용 공인 IP | 127.0.0.1 |
| `MEDIASOUP_LISTEN_IP` | 서버 바인딩 IP | 0.0.0.0 |
| `MEDIASOUP_PORT_RANGE_MIN` | WebRTC 포트 범위 시작 | 40000 |
| `MEDIASOUP_PORT_RANGE_MAX` | WebRTC 포트 범위 끝 | 40100 |
| `MEDIASOUP_LOG_LEVEL` | 로그 레벨 (debug/info/warn/error) | debug |

## 🔥 주요 기능

- **WebRTC SFU**: 다대다 미디어 스트리밍
- **Redis Pub/Sub**: 분산 환경 지원
- **자동 정리**: 연결 끊김 시 관련 객체 자동 삭제
- **환경별 설정**: .env.production을 통한 환경 설정

## 📡 Redis 채널

### 구독 채널 (Subscription)
- `mediasoup:router:create` - 라우터 생성
- `mediasoup:transport:create` - Transport 생성
- `mediasoup:transport:connect` - Transport 연결
- `mediasoup:producer:create` - Producer 생성
- `mediasoup:consumer:create` - Consumer 생성
- `mediasoup:consumer:resume` - Consumer 재개

### 발행 채널 (Publishing)
- `mediasoup:router:created` - 라우터 생성 완료
- `mediasoup:transport:created` - Transport 생성 완료
- `mediasoup:transport:connected` - Transport 연결 완료
- `mediasoup:producer:created` - Producer 생성 완료
- `mediasoup:consumer:created` - Consumer 생성 완료

## 🔧 개발환경 설정

### 로컬 개발
```bash
# .env.production 파일에서 개발용 설정
MEDIASOUP_ANNOUNCED_IP=127.0.0.1
MEDIASOUP_LOG_LEVEL=debug
```

### 프로덕션 배포
```bash
# .env.production 파일에서 운영용 설정
MEDIASOUP_ANNOUNCED_IP=공인_IP_주소
MEDIASOUP_LOG_LEVEL=warn
```

## 🐛 트러블슈팅

### 1. Redis 연결 실패
- Redis 서버가 실행 중인지 확인
- 방화벽에서 Redis 포트(6379) 허용 확인

### 2. WebRTC 연결 실패
- `MEDIASOUP_ANNOUNCED_IP`를 올바른 공인 IP로 설정
- 방화벽에서 UDP 포트 범위 허용 확인

### 3. Transport 타임아웃
- 네트워크 연결 상태 확인
- ICE/DTLS 연결 로그 확인

## 📞 지원

문제가 발생하면 로그를 확인하고 환경변수 설정을 다시 확인해주세요.
