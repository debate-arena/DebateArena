# 🔧 프론트엔드 데이터 처리 가이드

## 📡 개요

백엔드에서 받은 WebSocket 데이터를 프론트엔드에서 어떻게 가공하고 UI에 반영하는지에 대한 문서입니다.

---

## 🎯 데이터 처리 흐름

```
백엔드 원본 데이터 → 프론트 가공 → UI 상태 업데이트 → 화면 표시
```

---

## 📋 주요 처리 방식별 분류

### 1. **토론 진행 상태 관리**

#### 받는 데이터
```javascript
// /sub/debate/room/{roomId}/speak/start
{
  "speaker": "user01@email.com",
  "startTime": "2024-01-01T10:00:00Z", 
  "duration": 60000,
  "stage": "opinion"
}
```

#### 프론트 처리
```javascript
client.subscribe(`/sub/debate/room/${roomId}/speak/start`, (message) => {
  const msg = JSON.parse(message.body);
  
  // 현재 발언자 설정
  currentSpeaker.value = msg.speaker;
  
  // 발언 시작 시간 기록 (STT 턴 관리용)
  speakerStartAt.value = msg.startTime;
  
  // 발언 제한시간 설정
  speakingDuration.value = msg.duration;
  
  // 팀별 발언 상태 표시 (UI용)
  if (roomStore.leftTeam.find(p => p.userId === currentSpeaker.value)) {
    isLeftSpeaking.value = true;
    isRightSpeaking.value = false;
  } else {
    isLeftSpeaking.value = false;
    isRightSpeaking.value = true;
  }
  
  // 내가 발언자면 STT 자동 시작
  if (currentSpeaker.value === debateStore.myEmail) {
    stt.startOpinion(); // 또는 stt.startBattleAttack()
  }
  
  // 발언 타이머 시작
  startSpeakingTimer();
});
```

#### 가공된 상태 활용
```javascript
// 현재 발언자 표시
<div v-if="currentSpeaker === participant.userId" class="speaking-indicator">
  발언 중...
</div>

// 팀별 하이라이트
<div :class="{ 'team-speaking': isLeftSpeaking }" class="left-team">
  좌측 팀
</div>
```

### 2. **STT 메시지 처리**

#### 받는 데이터
```javascript
// /sub/debate/room/{roomId}/stt
{
  "user": "user01@email.com",
  "text": "저는 이 주제에 대해 반대합니다"
}
```

#### 프론트 처리
```javascript
stt.onSttMessage((payload) => {
  // 백엔드 데이터 파싱
  const body = payload?.data?.text ? payload.data : payload;
  const user = body?.user ?? "-";
  const text = body?.text ?? JSON.stringify(payload);
  
  // 토론 컨텍스트 추가
  const messageMode = currentStageIndex.value < 2 ? "normal" : "battle";
  const participant = roomStore.room?.participants.find(p => p.userId === user);
  const side = participant?.side;
  const team = side === "L" ? 0 : 1;
  
  // 턴 키 생성 (같은 발언 턴의 텍스트를 묶기 위함)
  const turnKey = makeTurnKey(user, speakerStartAt.value);
  let idx = msgIndexByTurn.get(turnKey);
  
  if (idx == null) {
    // 새로운 발언 턴 - 메시지 객체 생성
    messages.value.push({
      id: messageIdCounter++,
      type: "stt",
      mode: messageMode,              // "normal" | "battle"
      team: team,                     // 0=좌측, 1=우측
      timestamp: new Date(),
      sender: user,                   // 발언자 이메일
      sttText: "",                    // STT 텍스트 (누적)
      isAttacker: currentStageIndex.value >= 2 && 
                  currentSpeaker.value === user && 
                  isAttackPhase.value,
      isDefender: currentStageIndex.value >= 2 && 
                  currentSpeaker.value === user && 
                  isDefensePhase.value,
      summaryPending: true,           // AI 요약 대기중
      display: "stt",                 // "stt" | "summary"
      turnKey: turnKey                // 턴 식별자
    });
    
    idx = messages.value.length - 1;
    msgIndexByTurn.set(turnKey, idx);
    
    // AI 요약 대기 큐에 등록
    const queue = messageMode === "battle" 
      ? battleQueueByUser.get(user) ?? []
      : opinionQueueByUser.get(user) ?? [];
    queue.push(turnKey);
    if (messageMode === "battle") battleQueueByUser.set(user, queue);
    else opinionQueueByUser.set(user, queue);
  }
  
  // 같은 턴의 텍스트 누적
  const message = messages.value[idx];
  message.sttText = [message.sttText, text].filter(Boolean).join(" ");
  message.timestamp = new Date();
});
```

#### 화면 표시
```html
<!-- STT 메시지 UI -->
<div v-for="message in messages" :key="message.id" 
     v-if="message.type === 'stt'" 
     :class="['message', `team-${message.team}`]">
  
  <!-- 발언자 정보 -->
  <div class="message-header">
    <span class="sender">{{ getDisplayName(message.sender) }}</span>
    <span v-if="message.isAttacker" class="badge attack">공격</span>
    <span v-if="message.isDefender" class="badge defense">방어</span>
  </div>
  
  <!-- 토글 버튼 -->
  <button v-if="message.summaryText" 
          @click="message.display = message.display === 'summary' ? 'stt' : 'summary'">
    {{ message.display === 'summary' ? 'STT 보기' : '요약 보기' }}
  </button>
  
  <!-- 내용 표시 -->
  <div class="message-content">
    {{ displayedText(message) }}
  </div>
  
  <!-- 요약 대기 중 표시 -->
  <span v-if="message.summaryPending" class="loading">요약중…</span>
</div>
```

### 3. **AI 요약 처리**

#### 받는 데이터
```javascript
// /sub/debate/room/{roomId}/summaries/opinion
{
  "result": {
    "text": "발언자가 해당 주제에 대해 반대 의견을 명확히 표명함"
  },
  "user": "user01@email.com"
}
```

#### 프론트 처리
```javascript
stt.onOpinionSummary((payload) => {
  const user = payload?.user ?? "-";
  const summary = payload?.summary ?? 
                  payload?.text ?? 
                  payload?.result?.text ?? 
                  JSON.stringify(payload);
  
  // 대기 중인 메시지 찾아서 요약 첨부
  attachSummaryByQueue(user, summary, "normal");
});

function attachSummaryByQueue(user, summary, mode) {
  // 해당 사용자의 요약 대기 큐에서 가장 오래된 턴 가져오기
  const queueMap = mode === "battle" ? battleQueueByUser : opinionQueueByUser;
  const queue = queueMap.get(user) ?? [];
  const turnKey = queue.shift(); // 가장 오래된 미해결 턴
  queueMap.set(user, queue);
  
  if (!turnKey) return; // 붙일 대상 없음
  
  // 해당 턴의 메시지 찾아서 요약 첨부
  const idx = msgIndexByTurn.get(turnKey);
  if (idx == null) return;
  
  const message = messages.value[idx];
  message.summaryText = summary;
  message.summaryPending = false;
  message.display = "summary"; // 요약을 우선 표시
}
```

### 4. **투표 시스템 처리**

#### 받는 데이터
```javascript
// /sub/debate/room/{roomId}/vote/start
{
  "voteStartTime": "2024-01-01T10:10:00Z",
  "duration": 30000,
  "topic": "토론 주제"
}
```

#### 프론트 처리
```javascript
client.subscribe(`/sub/debate/room/${roomId}/vote/start`, (message) => {
  const msg = JSON.parse(message.body);
  
  // 투표 상태 활성화
  isVoteTime.value = true;
  voteStartAt.value = msg.voteStartTime;
  
  // 남은 시간 계산 및 타이머 시작
  startVoteTimer();
});

function startVoteTimer() {
  const serverStartMs = new Date(voteStartAt.value).getTime();
  const currentMs = Date.now();
  const elapsedMs = currentMs - serverStartMs;
  const remainingMs = Math.max(VOTE_DURATION - elapsedMs, 0);
  
  voteTimeLeft.value = Math.ceil(remainingMs / 1000);
  
  if (voteTimer.value) clearInterval(voteTimer.value);
  voteTimer.value = setInterval(() => {
    voteTimeLeft.value--;
    if (voteTimeLeft.value <= 0) {
      clearInterval(voteTimer.value);
      voteTimeLeft.value = 0;
    }
  }, 1000);
}
```

#### 투표 결과 처리
```javascript
// /sub/debate/room/{roomId}/vote/end
{
  "voteResult": 0,  // 0=좌측승, 1=우측승, 2=무승부
  "voteInfo": {
    "user01@email.com": 0,
    "user02@email.com": 1
  }
}
```

```javascript
client.subscribe(`/sub/debate/room/${roomId}/vote/end`, (message) => {
  const msg = JSON.parse(message.body);
  
  // 투표 결과를 메시지로 추가
  messages.value.push({
    id: messageIdCounter++,
    type: "vote",
    timestamp: new Date(),
    voteResult: msg.voteResult,
    voteInfo: msg.voteInfo,
    topic: debateSubject.value
  });
  
  // 투표 모달 닫기
  isVoteTime.value = false;
  
  // 다음 단계로 진행
  currentStageIndex.value++;
});
```

### 5. **공방전 공격 대상 선택**

#### 받는 데이터
```javascript
// /sub/debate/room/{roomId}/attack
{
  "attacker": "user01@email.com",
  "target": "user02@email.com",
  "selectedTime": "2024-01-01T10:05:30Z"
}
```

#### 프론트 처리
```javascript
client.subscribe(`/sub/debate/room/${roomId}/attack`, (message) => {
  const msg = JSON.parse(message.body);
  
  // 공격 정보 저장
  attacker.value = msg.attacker;
  selectedCurrentTarget.value = roomStore.room?.participants.find(
    p => p.userId === msg.target
  );
  
  // 공격 대상 선택 완료
  isSelectingTarget.value = false;
  
  // 타이머 정리
  if (selectTargetTimeLeftTimer.value) {
    clearInterval(selectTargetTimeLeftTimer.value);
    selectTargetTimeLeftTimer.value = null;
  }
});
```

### 6. **채팅 메시지 처리**

#### 받는 데이터
```javascript
// /sub/debate/room/{roomId}/chat
{
  "nickname": "사용자명",
  "text": "채팅 메시지", 
  "team": 0,
  "timestamp": "2024-01-01T10:00:00Z"
}
```

#### 프론트 처리
```javascript
client.subscribe(`/sub/debate/room/${roomId}/chat`, (message) => {
  const msg = JSON.parse(message.body);
  
  // 채팅 메시지 배열에 추가
  audienceMessages.value.push({
    id: Date.now(),
    nickname: msg.nickname,
    text: msg.text,
    team: msg.team,  // 0=좌측, 1=우측, null=시청자
    timestamp: new Date(msg.timestamp)
  });
  
  // 메시지 수 제한 (메모리 관리)
  if (audienceMessages.value.length > 100) {
    audienceMessages.value.shift();
  }
});
```

---

## 🔄 공통 처리 패턴

### 1. **에러 방어 처리**
```javascript
// 안전한 데이터 추출
const user = payload?.user ?? payload?.data?.user ?? "-";
const text = payload?.text ?? payload?.data?.text ?? JSON.stringify(payload);
```

### 2. **타임스탬프 처리**
```javascript
// 서버 시간 vs 클라이언트 시간 동기화
const serverStartMs = new Date(serverTime).getTime();
const currentMs = Date.now();
const elapsedMs = currentMs - serverStartMs;
const remainingMs = Math.max(duration - elapsedMs, 0);
```

### 3. **메모리 관리**
```javascript
// 오래된 메시지 제거
const LIMIT = 200;
if (messages.value.length > LIMIT) {
  const removed = messages.value.shift();
  // 관련 인덱스/큐 정리
  msgIndexByTurn.delete(removed.turnKey);
}
```

### 4. **상태 동기화**
```javascript
// 여러 상태를 한번에 업데이트
watch(currentSpeaker, (newSpeaker) => {
  // UI 상태 자동 업데이트
  updateSpeakingStates();
  updateSTTMode();
  updateTimerDisplay();
});
```

---

## 💡 핵심 처리 원칙

### 1. **백엔드 의존성**
- 모든 토론 진행은 백엔드 이벤트에 의존
- 프론트는 상태 표시 및 UX만 담당

### 2. **유연한 데이터 파싱**
- 다양한 응답 구조에 대응
- fallback 처리로 안정성 확보

### 3. **실시간 상태 관리**
- reactive한 상태 변수 활용
- watch/computed로 자동 업데이트

### 4. **사용자 경험 최적화**
- 로딩 상태 표시
- 실시간 피드백
- 에러 상황 대응

이러한 방식으로 백엔드의 단순한 데이터를 토론 시스템에 맞는 풍부한 UI 상태로 변환하여 사용자에게 제공합니다.
