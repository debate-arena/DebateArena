# Debate Arena Frontend

Vue 3 + TypeScript + Tailwind CSS + shadcn-vue 기반의 실시간 토론 플랫폼 프론트엔드입니다.

## 🚀 기술 스택

- **Framework**: Vue 3 (Composition API)
- **Language**: TypeScript
- **Styling**: Tailwind CSS 4
- **UI Components**: shadcn-vue
- **State Management**: Pinia
- **Router**: Vue Router 4
- **Build Tool**: Vite
- **HTTP Client**: Axios
- **Real-time**: WebSocket
- **Icons**: lucide-vue-next

## 📁 프로젝트 구조

```
src/
├── api/                 # API 클라이언트 설정
├── assets/              # 정적 자원
├── components/          # 재사용 컴포넌트
│   ├── ui/             # shadcn-vue UI 컴포넌트
│   └── matching/       # 매칭 관련 컴포넌트
│       ├── PlayerCountSelection.vue  # 글로벌 모드/진영 선택
│       └── TopicCard.vue            # 개별 주제 선택 카드
├── composables/         # Vue Composables
│   ├── useMatchingModals.ts    # 매칭 모달 상태 관리
│   ├── useMatchingTimer.ts     # 매칭 타이머 관리
│   ├── useMatchingActions.ts   # 매칭 액션 관리
│   └── useWebSocket.ts         # WebSocket 통신
├── constants/           # 상수 정의
├── pages/              # 페이지 컴포넌트
│   ├── Home.vue        # 홈 페이지
│   └── Matching.vue    # 매칭 페이지 (핵심 기능)
├── router/             # 라우터 설정
├── store/              # Pinia 스토어
│   ├── topicSet.ts     # 주제 세트 관리
│   └── matching.ts     # 매칭 상태 관리
├── types/              # TypeScript 타입 정의
│   └── matching.ts     # 매칭 관련 타입
├── utils/              # 유틸리티 함수
├── App.vue             # 루트 컴포넌트
└── main.ts             # 앱 진입점
```

## 🎯 핵심 기능: 매칭 시스템

### 📋 매칭 페이지 전체 플로우

#### 1. 페이지 진입 시 초기화
- **자동 설정**: 모든 주제가 "상관없음" 진영, "1:1, 2:2" 모드로 초기화
- **글로벌 체크박스**: 우측 패널의 글로벌 선택이 모든 주제에 적용
- **WebSocket 연결**: 실시간 매칭을 위한 연결 시도

#### 2. 사용자 선택 단계
- **글로벌 선택**: 우측 패널에서 모든 주제에 적용할 모드/진영 선택
  - 모드: 1:1, 2:2 (다중 선택 가능)
  - 진영: 선택1, 선택2, 상관없음 (단일 선택)
- **개별 선택**: 각 주제 카드에서 개별적으로 모드/진영 선택
- **실시간 동기화**: 글로벌 선택 ↔ 개별 선택 간 실시간 동기화

#### 3. 매칭 시작
- **조건 확인**: 최소 1개 주제 선택 + 최소 1개 모드 선택
- **시작 버튼**: 조건 만족 시 "매칭 시작" 버튼 활성화
- **주제 변경 경고**: 주제 변경 5분 전 경고 모달 표시

#### 4. 매칭 대기 중
- **타이머**: 경과 시간 표시 (최대 10분)
- **예상 시간**: 대기 시간 예측 표시
- **취소 버튼**: 매칭 중단 가능 (X 버튼)
- **상태 표시**: "매칭 대기 중..." + 스피너

#### 5. 매칭 결과 처리
- **성공 (50%)**: 매칭 성사 모달 → 수락 → 연결 중 모달 → 토론방 이동
- **타임아웃 (25%)**: 타임아웃 모달 → 재시도/취소 선택
- **주제 변경 (25%)**: 주제 변경 모달 → 자동 취소

#### 6. 연결 진행
- **연결 상태**: 4명의 사용자 아이콘 (회색 → 초록색 순차 변경)
- **진행률**: 연결된 사용자 수 실시간 표시
- **완료**: 모든 사용자 연결 시 토론방으로 자동 이동

### 🎨 UI/UX 특징

#### 레이아웃 구조
```
┌─────────────────────────────────────────────────────────────┐
│                    헤더 (홈으로 버튼)                        │
├─────────────────────────────────────────────────────────────┤
│  주제 변경 타이머 │  좌측: 매칭 선택 영역  │  우측: 제어 패널  │
│                 │  ├─ 글로벌 선택        │  ├─ 매칭 시작 버튼 │
│                 │  ├─ 주제 카드들       │  ├─ 타이머        │
│                 │  └─ (5개 주제)       │  ├─ 내 선택 목록   │
│                 │                      │  └─ 규칙/안내     │
└─────────────────────────────────────────────────────────────┘
```

#### 컴포넌트별 기능

**1. PlayerCountSelection (글로벌 선택)**
- 모든 주제에 적용되는 모드/진영 선택
- 체크박스 기반 다중/단일 선택
- 실시간 동기화 (글로벌 ↔ 개별)

**2. TopicCard (개별 주제)**
- 주제 정보 표시 (제목, 선택1, 선택2)
- 개별 모드/진영 선택 버튼
- 선택 상태에 따른 버튼 색상 변경
- 같은 진영 재클릭 시 선택 해제

**3. 우측 제어 패널**
- **매칭 시작 버튼**: 조건 만족 시 활성화, 매칭 중 스피너 표시
- **타이머**: 경과 시간 (mm:ss 형식)
- **내 선택 목록**: 선택된 주제와 모드 요약
- **규칙/안내**: 사용법 안내

#### 모달 시스템

**1. 매칭 성사 모달**
- 주제, 진영, 모드 정보 표시
- "수락" (큰 버튼) / "모든 매칭 취소" (작은 버튼)

**2. 연결 중 모달**
- 방 ID, 연결 상태 표시
- 4개 사용자 아이콘 (회색 → 초록색 순차 변경)
- 연결 진행률 실시간 업데이트

**3. 타임아웃 모달**
- 매칭 실패 안내
- "다시 매칭하기" / "취소" 선택

**4. 주제 변경 모달**
- 주제 변경으로 인한 자동 취소 안내
- "취소" 버튼만 제공 (재시도 불가)

### 🔧 기술적 구현 세부사항

#### 상태 관리 (Pinia Store)

**matching.ts 스토어**
```typescript
interface MatchingState {
  topicSelections: Map<number, TopicSelection>  // 주제별 선택
  globalModes: Set<PlayerMode>                  // 글로벌 모드
  globalStances: Set<Stance>                    // 글로벌 진영
  isMatching: boolean                           // 매칭 중 여부
  status: MatchingStatus                        // 매칭 상태
  elapsedTime: number                           // 경과 시간
  estimatedWaitTime?: number                    // 예상 대기 시간
  matchResult?: MatchResult                     // 매칭 결과
  error?: string                                // 오류 메시지
}
```

**주요 액션들**
- `initializeTopicSelections()`: 페이지 진입 시 초기화
- `toggleGlobalMode()`: 글로벌 모드 토글
- `setGlobalStance()`: 글로벌 진영 설정
- `syncGlobalState()`: 글로벌 ↔ 개별 동기화
- `startMatching()`: 매칭 시작
- `cancelMatching()`: 매칭 취소

#### Composables 패턴

**1. useMatchingModals (싱글톤)**
- 모든 매칭 관련 모달 상태 관리
- show/hide 함수 제공
- 디버깅 로그 포함

**2. useMatchingTimer**
- 매칭 타이머 관리 (1초마다 업데이트)
- 10분 타임아웃 처리
- 주제 변경 시 자동 취소 (정각 + 10초)

**3. useMatchingActions (싱글톤)**
- 매칭 시작/취소/성사/타임아웃 처리
- 시뮬레이션 확률 조정 (성공 50%, 타임아웃 25%, 주제 변경 25%)
- WebSocket 통신 로깅

#### 실시간 기능

**WebSocket 통신**
- 페이지 진입 시 자동 연결
- 매칭 요청/취소 전송
- 연결 상태 실시간 업데이트

**타이머 시스템**
- 주제 변경까지 남은 시간 (실시간 카운트다운)
- 매칭 경과 시간 (최대 10분)
- 자동 취소 타이머 (주제 변경 + 10초)

#### 아이콘 시스템

**lucide-vue-next UserIcon 사용**
- 유니코드 이모지(👤) 대신 SVG 아이콘 사용
- Tailwind 색상 클래스 정상 적용
- `text-green-500` (연결됨) / `text-gray-400` (연결 대기)

### 🎲 시뮬레이션 시스템

**매칭 결과 확률**
- **성공 (50%)**: 3초 후 매칭 성사 → 연결 진행
- **타임아웃 (25%)**: 3초 후 타임아웃 → 재시도 모달
- **주제 변경 (25%)**: 3초 후 주제 변경 → 자동 취소

**연결 진행 시뮬레이션**
- 4명 사용자, 1초마다 1명씩 연결
- 아이콘 색상: 회색 → 초록색 순차 변경
- 모든 사용자 연결 시 토론방 이동

### 🔄 데이터 플로우

```
1. 페이지 진입
   ↓
2. 주제 정보 로드 (topicSetStore.fetchTopicSets())
   ↓
3. 글로벌 상태 초기화 (1:1, 2:2, 상관없음)
   ↓
4. 개별 주제 선택 생성 (initializeTopicSelections)
   ↓
5. 사용자 선택 (글로벌 ↔ 개별 동기화)
   ↓
6. 매칭 시작 (조건 확인 → WebSocket 요청)
   ↓
7. 매칭 대기 (타이머 + 상태 표시)
   ↓
8. 결과 처리 (성공/타임아웃/주제 변경)
   ↓
9. 연결 진행 (아이콘 색상 변경)
   ↓
10. 토론방 이동
```

### 🎯 사용자 경험 (UX)

**직관적인 선택 시스템**
- 글로벌 선택으로 빠른 설정
- 개별 선택으로 세밀한 조정
- 실시간 동기화로 일관성 보장

**명확한 상태 표시**
- 매칭 시작 버튼 활성화 조건
- 타이머로 진행 상황 파악
- 아이콘 색상으로 연결 상태 확인

**안전한 취소 시스템**
- 매칭 중 언제든 취소 가능
- 페이지 이탈 시 자동 취소
- 주제 변경 시 안전한 자동 취소

**반응형 피드백**
- 버튼 색상 변경
- 스피너 애니메이션
- 모달을 통한 명확한 안내

## 🛠️ 개발 환경 설정

### 1. 의존성 설치
```bash
npm install
```

### 2. 개발 서버 실행
```bash
npm run dev
```

### 3. 빌드
```bash
npm run build
```

### 4. 린팅
```bash
npm run lint
```

## 🔧 주요 기능

### 1. 토픽 세트 관리
- **useTopicSetController**: 토픽 세트 자동 교체 및 서버 시간 동기화
- **useTimer**: 공통 타이머 로직 (1초마다 업데이트)

### 2. 매칭 시스템
- **useMatchingStore**: 매칭 상태 관리 (글로벌/개별 선택)
- **useWebSocket**: 실시간 매칭 통신

### 3. UI 컴포넌트
- **shadcn-vue**: 일관된 디자인 시스템
- **반응형 레이아웃**: 데스크톱 최적화 (min-width: 1200px)

## 📋 환경 변수

`.env` 파일을 생성하고 다음 변수들을 설정하세요:

```env
# API 설정
VITE_API_BASE_URL=http://localhost:8000

# WebSocket 설정
VITE_WS_BASE_URL=ws://localhost:8000

# 개발 환경 설정
VITE_DEV_MODE=true
```

## 🎨 디자인 시스템

### 색상 테마
- **Neutral 색상**: 기본 테마
- **CSS 변수**: HSL 기반 색상 시스템
- **다크모드**: 자동 지원

### 컴포넌트 규칙
- 모든 UI 컴포넌트는 `shadcn-vue` 사용
- 네이티브 HTML 버튼 대신 `Button` 컴포넌트 사용
- 일관된 스페이싱과 타이포그래피

## 🔄 상태 관리

### Pinia 스토어
1. **topicSet**: 토픽 세트 및 서버 시간 관리
2. **matching**: 매칭 상태 및 선택 관리

### Composables
1. **useTopicSetController**: 토픽 세트 자동 관리
2. **useTimer**: 공통 타이머 로직
3. **useWebSocket**: 실시간 통신

## 🚀 배포

### 프로덕션 빌드
```bash
npm run build
```

### 환경별 설정
- **개발**: `VITE_DEV_MODE=true`
- **프로덕션**: 환경변수로 API URL 설정

## 📝 코딩 컨벤션

- **TypeScript**: 엄격한 타입 체크
- **Vue 3**: Composition API 사용
- **ESLint**: 코드 품질 관리
- **Prettier**: 코드 포맷팅

## 🤝 기여 가이드

1. 브랜치 생성: `feature/기능명`
2. 커밋 메시지: `feat: 기능 설명`
3. PR 생성 시 코드 리뷰 필수
4. 테스트 코드 작성 권장
