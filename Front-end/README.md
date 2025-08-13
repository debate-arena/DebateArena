# 매칭 기능 개발 일지

## 🐛 오늘 겪었던 오류들과 해결 방법

### 1. 모달 이벤트 차단 문제
**문제**: 수락/거절 버튼이 전혀 클릭되지 않음
**원인**: Dialog 컴포넌트의 `:modal="true"` 설정이 이벤트를 완전히 차단
**해결**: `:modal="false"`로 변경하여 이벤트 차단 해제

```vue
<!-- Before -->
<Dialog :open="isOpen" :modal="true">

<!-- After -->
<Dialog :open="isOpen" :modal="false">
```

### 2. 타이머 자동 닫기 문제
**문제**: 10초 후에도 모달이 자동으로 닫히지 않음
**원인**: `useMatchingState`에서 동적 import로 모달 닫기 시도했지만 실패
**해결**: `Matching.vue`에서 `watch`로 에러 상태 감시하여 모달 닫기

```typescript
// useMatchingState.ts - 타이머에서 에러만 설정
if (acceptTimeLeft.value <= 0) {
  stopAcceptTimer()
  setError('수락 시간이 만료되었습니다.')
  console.log('⏰ 수락 타이머 만료됨')
}

// Matching.vue - 에러 감시하여 모달 닫기
watch(() => matchingState.matchingState.value.error, (error) => {
  if (error === '수락 시간이 만료되었습니다.') {
    console.log('⏰ 타이머 만료로 모달 닫기')
    modals.hideMatchCompleteModal()
    matchingState.clearError()
  }
})
```

### 3. 버튼 클릭 이벤트 문제
**문제**: Button 컴포넌트가 이벤트를 차단하는 것 같음
**해결**: 일반 `div`로 변경하고 직접 스타일링

```vue
<!-- Before -->
<Button @click="handleAccept" class="flex-1">

<!-- After -->
<div @click="handleAccept" class="flex-1 bg-debate-random px-4 py-2 rounded-md cursor-pointer">
```

### 4. 상태 초기화 문제
**문제**: 페이지 진입 시 이미 매칭 중인 상태로 표시됨
**해결**: `showTestMatchModal`에서 `matchingState.reset()` 호출

```typescript
const showTestMatchModal = () => {
  // 매칭 상태 초기화
  matchingState.reset()
  // ... 나머지 로직
}
```

## 🔧 각 함수/컴포저블/컴포넌트 역할

### 1. MatchingModal.vue (매칭 성사 모달)
**역할**: 매칭 성사 시 표시되는 모달
**주요 기능**:
- 주제 정보, 유저 상태, 타이머 표시
- 수락/거절 버튼 처리
- 10초 타이머 및 프로그레스바

**핵심 함수들**:
```typescript
// 버튼 클릭 핸들러
const handleAccept = () => {
  console.log('🎯 MatchingModal - 수락 버튼 클릭됨!')
  emit('accept')
}

const handleReject = () => {
  console.log('🎯 MatchingModal - 거절 버튼 클릭됨!')
  emit('reject')
}

// 선택지 이름 가져오기
const option1Name = computed(() => currentTopic.value?.option1 || '선택1')
const option2Name = computed(() => currentTopic.value?.option2 || '선택2')
```

### 2. useMatchingState.ts (매칭 상태 관리)
**역할**: 매칭 관련 상태와 타이머 관리
**주요 상태**:
```typescript
const matchingState = ref<MatchingState>({...})
const acceptTimeLeft = ref(10)
const currentMatchId = ref('')
const isConnecting = ref(false)
```

**핵심 함수들**:
```typescript
// 타이머 관리
const startAcceptTimer = () => {
  acceptTimeLeft.value = 10
  acceptTimer = setInterval(() => {
    acceptTimeLeft.value -= 0.2
    if (acceptTimeLeft.value <= 0) {
      stopAcceptTimer()
      setError('수락 시간이 만료되었습니다.')
    }
  }, 200)
}

// 상태 변경
const setMatched = () => { matchingState.value.status = 'matched' }
const setConnecting = () => { 
  matchingState.value.status = 'connecting'
  isConnecting.value = true 
}
const reset = () => { /* 모든 상태 초기화 */ }
```

### 3. useMatchingModals.ts (모달 상태 관리)
**역할**: 모든 매칭 관련 모달의 상태 관리
**주요 상태**:
```typescript
const modalState = ref<ModalState>({
  isMatchCompleteModalOpen: false,
  isTimeoutModalOpen: false,
  // ...
})
const matchModalData = ref<MatchModalData>({...})
const matchUsers = ref<Map<string, MatchUser>>(new Map())
```

**핵심 함수들**:
```typescript
const showMatchCompleteModal = (data: MatchModalData, users: MatchUser[]) => {
  modalState.value.isMatchCompleteModalOpen = true
  matchModalData.value = data
  // 유저 데이터 설정
}

const hideMatchCompleteModal = () => {
  modalState.value.isMatchCompleteModalOpen = false
}
```

### 4. useMatchingActions.ts (매칭 액션 처리)
**역할**: 매칭 관련 모든 액션 처리
**핵심 함수들**:
```typescript
// 매칭 성사 처리
const handleMatchSuccess = (data: any) => {
  matchingState.currentMatchId.value = data.matchId
  matchingState.setMatched()
  matchingState.startAcceptTimer()
  modals.showMatchCompleteModal(modalData, users)
}

// 매칭 수락
const acceptMatch = () => {
  matchingState.setConnecting()
  webSocket.sendMatchAcceptance(matchId, true)
  matchingState.startAcceptTimer()
}
```

### 5. Matching.vue (메인 매칭 페이지)
**역할**: 매칭 페이지의 메인 컴포넌트
**핵심 함수들**:
```typescript
// 모달 핸들러들
const handleModalAccept = () => {
  matchingState.setConnecting()
  webSocket.sendMatchAcceptance(matchId, true)
  matchingState.startAcceptTimer()
}

const handleModalReject = () => {
  webSocket.sendMatchAcceptance(matchId, false)
  modals.hideMatchCompleteModal()
  matchingState.stopAcceptTimer()
}

// 테스트 함수
const showTestMatchModal = () => {
  matchingState.reset()
  matchingState.setMatched()
  matchingState.currentMatchId.value = 'test-match-id'
  matchingState.startAcceptTimer()
  modals.showMatchCompleteModal(modalData, testUsers)
}
```

## 🎨 UI 컴포넌트별 역할

### 1. 유저 상태 아이콘
```vue
<!-- 수락 후: 진영별 프로필 아이콘 -->
<img v-if="user.accept === true" :src="debateLeftIcon" />

<!-- 거절: 회색 X 아이콘 -->
<X v-else-if="user.accept === false" class="text-debate-random" />

<!-- 연결 중: 회색 사람 아이콘 -->
<UserIcon v-else class="text-debate-random" />
```

### 2. 버튼 상태
```vue
<!-- 수락 전: 수락 + 거절 버튼 -->
<div v-if="!isConnecting" class="flex gap-2">
  <div @click="handleAccept" class="bg-debate-random">수락</div>
  <div @click="handleReject" class="bg-white text-black">거절</div>
</div>

<!-- 수락 후: 수락 완료 버튼만 -->
<div v-if="isConnecting" class="bg-debate-random">수락 완료</div>
```

### 3. 타이머 및 프로그레스바
```vue
<!-- 시간 표시 -->
<span>{{ Math.ceil(timeLeft) }}초</span>

<!-- 프로그레스바 -->
<div class="bg-debate-random" :style="{ width: `${((10 - timeLeft) / 10) * 100}%` }">
```

## 🔄 데이터 플로우

### 1. 매칭 성사 → 모달 표시
```
WebSocket 메시지 → handleMatchSuccess → showMatchCompleteModal → 모달 표시
```

### 2. 수락 버튼 클릭
```
MatchingModal.handleAccept → Matching.handleModalAccept → 
matchingState.setConnecting → WebSocket 전송 → 타이머 시작
```

### 3. 타이머 만료
```
타이머 0초 → setError → Matching.watch → hideMatchCompleteModal → 모달 닫기
```

## 🎯 핵심 해결 방법들

### 1. 이벤트 차단 해결
- `:modal="false"` 설정
- `pointer-events: auto !important` 강제 적용
- `z-index: 9999 !important` 최상위 레이어

### 2. 상태 동기화 해결
- `matchingState.reset()` 호출로 초기화
- `watch`로 에러 상태 감시
- try-catch로 안전한 에러 처리

### 3. UI 반응성 해결
- `computed` 속성으로 반응형 데이터
- `v-if`로 조건부 렌더링
- 적절한 이벤트 정리

---

**개발 완료**: 2024년 현재  
**상태**: ✅ 모든 기능 정상 작동  
**테스트**: ✅ 수락/거절/타이머 모두 정상
