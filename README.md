# 8월 8일 평가
## 목차
1. 프로젝트 전체 진행 상황
2. 프론트엔드 관련 정보 및 현황
3. 백엔드 관련 정보 및 현황
4. AI 관련 정보 및 현황

## 프로젝트 전체 진행 상황
### 목표
0. 8월 8일 오전 프로젝트 진행 상황
    - MVP 수준(8/11 완료 예정) : 80 / 100
        - Front : 70 / 100
        - Back : 80 / 100
        - AI : 100 / 100

    - 실제 배포 수준(8/14 완료 예정) : 60 / 100
        - Front : 60 / 100
        - Back : 60 / 100
        - AI : 80 / 100

1. WebRTC, Socket 직접 구현 -> 성공
2. MVP 배포(8월 12일 오후 14시까지 목표)
3. CI/CD 기본 설정 완료
4. 개발은 8월 14일 14시까지 완성
5. 8월 14일 이후에도 기능이 추가될 수 있음
6. 8/15 ~ 8/17 22시까지 발표 및 영상 완성, 추가 기능 구현은 고려 

# Frontend
### 주요 기능
- 🔐 OAuth2 기반 Google 로그인
- 🎯 실시간 매칭 시스템
- 🎤 실시간 STT (Speech-to-Text)
- 🎨 현대적이고 반응형 UI
- 🔄 자동 토픽 교체 시스템
- 📊 실시간 통신 (WebSocket + WebRTC)

## 🛠️ 기술 스택

### 핵심 프레임워크
- **Vue.js** 3.5.17 - Progressive JavaScript Framework
- **TypeScript** 5.8.3 - 정적 타입 지원
- **Vite** 7.0.4 - 빠른 빌드 도구
- **Pinia** 3.0.3 - 상태 관리
- **Vue Router** 4.5.1 - 클라이언트 사이드 라우팅

### UI/UX 라이브러리
- **Tailwind CSS** 4.1.11 - 유틸리티 퍼스트 CSS 프레임워크
- **shadcn-vue** 2.2.0 - 재사용 가능한 UI 컴포넌트
- **lucide-vue-next** 0.525.0 - 아이콘 라이브러리
- **vee-validate** 4.15.1 - 폼 검증
- **zod** 3.25.76 - 스키마 검증

### 실시간 통신
- **@stomp/stompjs** 7.1.1 - STOMP WebSocket 클라이언트
- **mediasoup-client** 3.14.0 - WebRTC 미디어 통신
- **axios** 1.11.0 - HTTP 클라이언트

## 🎯 주요 기능

### 1. OAuth2 로그인 시스템
- Google 로그인 지원
- 자동 인증 확인
- 닉네임 설정 필수 시스템
- Pinia 기반 전역 상태 관리

### 2. 실시간 매칭 시스템
- WebSocket 기반 실시간 매칭
- 토픽 선택 및 스탠스(찬성/반대/랜덤) 선택
- 플레이어 모드(1:1, 2:2) 지원
- 매칭 성공 시 자동 방 생성 및 라우팅

### 3. 토픽 관리 시스템
- 자동 토픽 교체 (1시간마다)
- 실시간 타이머 및 카운트다운
- 서버에서 토픽 데이터 실시간 가져오기

### 4. STT (Speech-to-Text) 시스템
- Web Speech API 기반 실시간 음성 인식
- STT 세그먼테이션 (문장, 단어, 시간 기반)
- WebSocket 통신 (STOMP 프로토콜)

### 5. UI/UX 시스템
- Tailwind CSS 기반 반응형 디자인
- shadcn-vue 컴포넌트 라이브러리
- 현대적이고 사용자 친화적 인터페이스
- 다크/라이트 모드 지원

## 📈 진행상태

| 기능 | 구현 상태 | 완성도 | 주요 특징 |
|------|-----------|--------|-----------|
| **로그인 시스템** | ✅ 완료 | 95% | OAuth2, 닉네임, 자동 인증 |
| **토픽 시스템** | ✅ 완료 | 90% | 자동 교체, 실시간 타이머 |
| **매칭 시스템** | ✅ 완료 | 85% | 실시간 매칭, 방 생성 |
| **STT 시스템** | ✅ 완료 | 80% | 실시간 음성 인식, WebSocket |

## Backend
### 핵심 기능 구현 현황

1. **WebSocket 기반 실시간 STT 메시지 처리** (완료)

* Opinion STT / Battle STT 구분 처리
* 실시간 WebSocket 통신을 통해 STT 메시지 수집 및 브로드캐스팅
* `/debate/{roomId}/stt/opinion`, `/debate/{roomId}/stt/battle` 엔드포인트 구현 (`DebateApiController`)

2. **토론방 생성 및 관리 시스템** (진행중)

* Redis 기반 토론방 상태 관리 (`DebateRedisInfo`, `DebateRedisRepository`)
* 토론방 생성, 상태 업데이트, TTL 관리 기능 구현
* `RoomManager`를 통한 개별 토론방 상태 추적

3. **실시간 매칭 시스템** (완료)

* WebSocket 기반 매칭 요청 처리: `/match/request`, `/match/acceptance`
* 스케줄러를 통한 고정 간격 매칭 상태 브로드캐스트
* 매시간 토픽 자동 갱신 및 매칭 큐 리셋

4. **토픽 관리 시스템** (완료)

* Redis 기반 캐싱 및 토픽 조회 API 제공
* 시간 기반 토픽 교체 시스템
* Swagger UI를 통한 API 문서화

5. **시그널링 서비스** (진행중)

* WebRTC 시그널링 
* WebRTC 연결된 참가자 정보 관리

6. **미디어 스트리밍 서비스** (진행중)
* WebRTC 미디어 스트리밍
* Transport 자원 관리
* 미디어 제어
* 연결에 관한 각종 Event 발생 시 Redis pub/sub 으로 알림

7. **AI 기반 토론 분석 기능** (예정)

* STT 텍스트 누적 저장 구조 구현 완료
* AI 서버 연동을 위한 `RestClient` 기반 통신 인프라 준비

---

### 시스템 아키텍처 및 기술 구현

**🔹 마이크로서비스 구조**

| 서비스 이름         | 포트   | 주요 기능                  |
| -------------- | ---- | ---------------------- |
| `debate-arena` | 8080 | OAuth2 + JWT 인증, 토픽 관리 |
| `matching`     | 8081 | WebSocket 기반 매칭 서비스    |
| `debate`       | 8082 | 토론방 관리, STT 처리         |
| `signaling`    | 8083 | Webrtc 시그널링 및 방 WebRTC연결 정보 관리         |
| `mediasoup`    | 40000-40100(임시) | 미디어 스트리밍 SFU 서버 |
| `frontend`     | 3000 | React 18.2.0 기반 UI     |

---

### 데이터 저장 및 캐싱

* **MySQL**: JPA 기반 관계형 데이터 저장 (토픽, 유저, 토론방)
* **Redis**: 상태 관리, 토픽 캐싱, 세션 관리, pub/sub
* **ConcurrentHashMap**: 인메모리 Room 상태 매니저

---

### 인증 및 보안

* **OAuth2**: Google 소셜 로그인 구현
* **JWT**: 토큰 기반 인증 시스템
* **Spring Security**: 보안 필터 체인 구성

---

### 실시간 통신

* **WebSocket + STOMP**: 실시간 매칭 및 토론 데이터 처리
* **SimpMessagingTemplate**: 메시지 브로드캐스팅
* **@MessageMapping**: 클라이언트 메시지 라우팅 처리
* **WebRTC**: WebRTC 미디어 스트리밍

---

### 발생 이슈 및 해결 사례

| 이슈                    | 해결 방법                                    |
| --------------------- | ---------------------------------------- |
| **Docker 네트워크 통신 문제** | `host.docker.internal` 사용하여 컨테이너 간 통신 해결 |
| **서비스 간 의존성 문제**      | `depends_on` 설정으로 시작 순서 제어               |
| **Redis 인증 문제**       | Spring Boot에서 Redis 패스워드 설정 및 연결 인증 구현   |
| **WebSocket 인증 문제**   | JWT 인증 및 사용자 라우팅 처리 구현 완료                |
| **미디어 스트리밍 자원 관리**   | signaling 서버에서 소켓 연결 해제시 Mediasoup 에 자원 해제 처리 |

---

### 개발 환경 및 도구

** 기술 스택 **

* **Backend**: Spring Boot 3.5.3, Java 21, Node v18.17.1
* **Database**: MySQL 8.0.43, Redis 8.0.3
* **Message Queue**: Spring WebSocket + STOMP
* **Container**: Docker, Docker Compose
* **Build Tool**: Gradle
* **Library**: Mediasoup 3.16.7

---

** API 문서화 **

* **Swagger / OpenAPI 3.0** 적용
* 접근 URL:

  * `debate-arena`: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
  * `matching`: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
  * `debate`: [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)

---

** 개발 편의성 향상 도구 **

* **Docker 분리 관리**: 인프라와 앱 컨테이너 분리
* **자동화 스크립트**:

  * `start-infra.sh`, `start-apps.sh`, `build-all.sh`
* **실시간 로그 통합 모니터링**: Docker Compose 로그 활용

---

### 🏗️ 시스템 운영 구조

** 서비스 의존성 흐름**

```
MySQL & Redis (인프라)
    ↓
debate-arena (8080) - 인증 및 토픽 서비스
    ↓
matching (8081) - 매칭 서비스
    ↓
debate (8082) - 토론 및 STT 서비스
    ↓
frontend (3000) - 사용자 인터페이스
```

---

** 📡 데이터 흐름 (Data Flow) **

1. **사용자 로그인**: OAuth2 (Google)
2. **JWT 발급 및 인증**
3. **WebSocket 매칭 요청**
4. **WebSocket 매칭 성사**
5. **WebRTC 연결 요청**
6. **WebRTC 연결 성사**
7. **토론방 생성 및 입장**
8. **토론 진행**
9. **실시간 STT 메시지 송수신**
10. **토론 결과**

---


### AI
### 1. FastAPI로  AI 서버 구축 완료
### 2. AI 기술 활용
1. 주제에 대한 주장 요약(완료)
    - 토론 참여자의 STT 데이터를 기반으로 주장 내용 요약

2. 공방전에 대한 주장 요약 + 방어 점수 부여 (완료)
    - 공격과 방어 STT를 받아 각자 요약, 해당 요약본을 활용하여 AI가 잘 방어했는지 1~10점 사이로 평가
 
3. 전체 내용 요약 (완료)
    - 진행된 모든 토론 내용(요약본들)을 받아 다시 한 번 진영별로 전체 요약

4. 무승부 시 AI 청중단 개입 (완료)
    - 사용자 간 찬/반이 무승부가 되었을 때 가상의 청중단(50명)이 개입하여 승자가 누구인지 판단을 내림
    - 판단은 투표 형식으로 29 : 21 같은 형식으로 제공
    - 랜덤으로 청중 한 명을 뽑아 해당 인물의 특성과 어떤 이유로 투표를 하였는지 알려줌

5. (예정) 청중단의 판정을 기반으로 기억 학습 구현
    - 청중들의 백터 일정 수준 보정

### 3. AI 외의 기술 구현
- ChromaDB를 활용한 AI 임베딩 벡터 검색 인프라 구현
    - 각 청중의 성격, 성향, 나이 등의 설명을 GPT 임베딩하여 3072차원 벡터로 변환 후 저장
    - 임베딩된 토론 요약문과 청중 벡터 간 유사도를 계산해 AI 판정 기반 마련

### 4. 발생 이슈
- AI 청중단의 판정 함수를 돌리면 첫 판정 시 판정단이 0:50 처럼 한쪽으로 쏠림(해결 완료)
- 들어오는 순서에 따라 판정 요동침 (해결 완료)
- GMS 토큰 관리 문제 (해결 완료)

### 3. GMS 활용
    1. 논리 요약: gpt-4.1-mini
    2. 공방전 요약 + 반박 평가: gpt-4.1-mini
    3. 최종 전체 요약: gpt-4.1
    4. 요약 임베딩: gpt-3 text-embedding-large (3072차원)
    5. 대표자 투표 이유 생성: gpt-4.1-mini (temperature 0.7)

### 4. 추가 고려 사항
    - AI 청중의 모델화(Tensorflow를 활용하여 구현)
    - 토큰 최소화
    - API 반환 시간 최소화(요약 및 판정 최적화)

### ~~Whisper 모델(base, small or fast-whisper 사용 방안 ...) 비교 및 STT 모델 선정, 테스팅~~ 
- STT는 클라이언트에서 작동