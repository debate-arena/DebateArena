<template>
  <div>
    <!-- 에러 메시지 (불투명 토스트) -->
    <div v-if="matchingStore.error" class="fixed bottom-6 right-6 z-50">
      <Alert class="max-w-sm bg-card border border-border shadow-xl">
        <AlertCircle class="h-4 w-4" />
        <AlertTitle>알림</AlertTitle>
        <AlertDescription>{{ matchingStore.error }}</AlertDescription>
      </Alert>
    </div>

    <!-- 메인 콘텐츠 -->
    <div class="py-8">
      <!-- 매칭 선택 영역 - idle 상태일 때만 표시 -->
      <MatchingSelectionArea 
        v-show="matchingStore.status === 'idle'"
        :is-starting-match="isStartingMatch"
        @start-matching="handleStartMatching"
        @cancel-matching="handleCancelMatching"
      />

      <!-- 게임 통계 패널 - waiting 상태일 때만 표시 -->
      <div v-show="matchingStore.status === 'waiting'" class="mt-8">
        <GameStatsPanel />
      </div>

      <!-- 매칭 결과 패널 - matched 상태일 때만 표시 -->
      <MatchResultPanel
        v-if="matchingStore.status === 'matched'"
        :is-open="true"
        :topic-title="currentMatchTopicTitle"
        :stance="currentUserStance"
        :mode="currentMatchMode"
        :topic-id="(currentMatchTopicId ?? 0)"
        :total-count="currentMatchTotalCount"
        :time-left="matchingStore.acceptTimeLeft"
        :is-connecting="false"
        :last-accepted-stance="matchingStore.lastAcceptedStance"
        :stance-acceptance="matchResultState.stanceAcceptance.value"
        :self-acceptance="selfAcceptance"
        @accept="handleModalAccept"
        @reject="handleModalReject"
        @update:is-open="() => {}"
      />
    </div>
    
    <!-- 모달들 (타임아웃/주제변경 알림 미사용) -->
    
    <LoginRequiredModal 
      v-model:open="modals.modalState.value.isLoginRequiredModalOpen" 
      @close="modals.hideLoginRequiredModal()" 
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { useMatchingStore } from '@/store/matching'
import { useTopicSetStore } from '@/store/topicSet'
import { useMatchingTimer } from '@/composables/useMatchingTimer'
import { useMatchingActions } from '@/composables/useMatchingActions'
import { useTopicSetController } from '@/composables/useTopicSetController'
import { useMatchingModals } from '@/composables/useMatchingModals'
import { useWebSocket } from '@/composables/useWebSocket'
import { useMatchResultState } from '@/composables/useMatchResultState'
import { useAuthStore } from '@/store/auth'
import { useRoomStore } from '@/store/roomStore'
import { AlertCircle } from 'lucide-vue-next'

// Components
import MatchingSelectionArea from '@/components/matching/MatchingSelectionArea.vue'
import MatchResultPanel from '@/components/matching/MatchResultPanel.vue'
import GameStatsPanel from '@/components/matching/GameStatsPanel.vue'
// TimeoutModal, TopicChangeModal, HourWarningModal 미사용 처리
// import TimeoutModal from '@/components/matching/TimeoutModal.vue'
// import TopicChangeModal from '@/components/matching/TopicChangeModal.vue'
// import HourWarningModal from '@/components/matching/HourWarningModal.vue'
import LoginRequiredModal from '@/components/matching/LoginRequiredModal.vue'

// Types
import type { WebSocketMessage } from '@/types/matching'

// Utils
import { 
  teamToStanceFlexible, 
  getTotalCount,
  processMatchInvitation,
  processMatchResult,
  processAcceptanceStatus
} from '@/utils/matching'

// Stores & Composables
const router = useRouter()
const matchingStore = useMatchingStore()
const topicSetStore = useTopicSetStore()
const authStore = useAuthStore()
const roomStore = useRoomStore()

// Controllers (전역 싱글톤 타이머라 한 번만 구독되도록 유지)
useTopicSetController()

// Composables
const modals = useMatchingModals()
const actions = useMatchingActions()
const webSocket = useWebSocket()
const matchResultState = useMatchResultState()
const { startMatchingTimer, stopMatchingTimer, startAcceptTimer, stopAcceptTimer } = useMatchingTimer()

// State
const isStartingMatch = ref(false)
const selfAcceptance = ref<'pending' | 'accepted' | 'rejected'>('pending')
// 중복 요청 방지용 키 및 수락/거절 전송 락
const lastMatchRequestHash = ref<string | null>(null)
const sentAcceptanceForMatchIds = new Set<string>()

// Computed Properties
const currentMatchTopicId = computed<number | null>(() => matchingStore.currentMatchTopicId)
const currentMatchMode = computed(() => matchingStore.currentMatchMode || '1:1')
const currentUserStance = computed(() => teamToStanceFlexible(matchingStore.currentUserTeam))
const currentMatchTopicTitle = computed(() => {
  const id = currentMatchTopicId.value
  const topic = id ? topicSetStore.currentSet?.topics.find(t => t.id === id) : undefined
  return topic?.title || (id ? `주제 ${id}` : '주제')
})
const currentMatchTotalCount = computed(() => getTotalCount(currentMatchMode.value))
// 팀 정원(한 진영 최대 인원) 설정: 매칭 초대/모드 확정 시 동기화
watch(currentMatchMode, (mode) => {
  const perTeam = getTotalCount(mode) / 2
  matchResultState.setTeamSize(perTeam)
}, { immediate: true })

// WebSocket Message Handler
const handleWebSocketMessage = (data: WebSocketMessage) => {
  switch (data.type) {
    case 'MATCH_INVITATION':
      handleMatchInvitation(data)
      break
      
    case 'ACCEPTANCE_STATUS':
      handleAcceptanceStatus(data)
      break
      
    case 'MATCH_RESULT':
      handleMatchResult(data)
      break
      
    case 'ERROR':
      if (data.status === 'error') {
        actions.handleError(data.data?.message || '알 수 없는 에러')
      }
      break
      
    default:
      console.log('⚠️ 알 수 없는 메시지 타입:', data.type)
  }
}

// WebSocket Handlers
const handleMatchInvitation = (data: WebSocketMessage) => {
  if (data.status === 'error') {
    actions.handleError(data.data?.message || '매칭 초대장 처리 중 오류가 발생했습니다.')
    return
  }
  
  if (data.status === 'success') {
    const invitation = processMatchInvitation(data, topicSetStore)
    
    // 스토어에 매칭 정보 저장
    matchingStore.setCurrentMatchId(invitation.matchId)
    matchingStore.setCurrentUserTeam(invitation.team)
    matchingStore.setCurrentMatchTopicId(invitation.topicId)
    matchingStore.setCurrentMatchMode(invitation.mode)
    matchingStore.setMatched()
    
    // MatchResultPanel 상태 초기화
    matchResultState.resetStanceAcceptance()
    // 초대 시점의 모드 기반 팀 정원
    const perTeam = getTotalCount(invitation.mode) / 2
    matchResultState.setTeamSize(perTeam)
    selfAcceptance.value = 'pending'
    
    // 수락 타이머 시작
    matchingStore.startAcceptTimer()
    startAcceptTimer()
  }
}

const handleAcceptanceStatus = (data: WebSocketMessage) => {
  if (data.status === 'error') return
  
  if (data.status === 'success') {
    const { accept, stance } = processAcceptanceStatus(data)

    if (accept) {
      matchingStore.setLastAcceptedStance(stance)
    }
    
    matchResultState.updateStanceAcceptance(stance, accept)
  }
}

const handleMatchResult = (data: WebSocketMessage) => {
  if (data.status === 'error') return

  // 공통 처리 함수 (FAIL 시 재사용)
  const handleFail = () => {
    // FAIL 경로 처리
    matchResultState.resetStanceAcceptance()
    matchingStore.stopAcceptTimer()
    stopAcceptTimer()

    if (selfAcceptance.value === 'accepted') {
      // 내가 수락한 경우: 즉시 waiting 화면으로 전환 (matched -> waiting 강제)
      matchingStore.setStatus('waiting')
      // 타이머 재가동을 위해 isMatching만 보장 (elapsedTime은 누적 유지)
      matchingStore.isMatching = true
      // 타임아웃 모달 미사용: 콜백 없이 타이머만 유지
      startMatchingTimer()
      // 초대장 상태 정리 (패널 언마운트 이후로 지연하여 topicId 0 로그 방지)
      nextTick(() => {
        matchingStore.clearInvitation()
        matchResultState.resetStanceAcceptance()
      })
    } else {
      // 내가 거절/미응답: 초기 화면 복귀(선택값 보존), 소켓 종료
      matchingStore.cancelMatching()
      stopMatchingTimer()
      webSocket.disconnect()
      if (selfAcceptance.value === 'pending') {
        // 미응답을 로컬 거절로 처리
        selfAcceptance.value = 'rejected'
      }
      // 초대장 상태 정리
      matchingStore.clearInvitation()
      matchResultState.resetStanceAcceptance()
    }
  }

  if (data.status === 'success') {
    const result = processMatchResult(data)
    
    if (result.success && result.roomId) {
      // 방 정보가 함께 온다면 RoomStore에 즉시 반영 (보내기 전 준비)
      try {
        const participants = [
          ...((result as any).leftTeam || (data.data.firstTeam ?? [])).map((m: any) => ({ userId: m.email, displayName: m.nickname, side: 'L' as const })),
          ...((result as any).rightTeam || (data.data.secondTeam ?? [])).map((m: any) => ({ userId: m.email, displayName: m.nickname, side: 'R' as const })),
        ]
        roomStore.setRoom({ roomId: result.roomId, participants })
      } catch (e) {
        console.warn('RoomStore 초기화 중 경고(무시 가능):', e)
      }

      handleMatchSuccess(result.roomId)
    } else {
      handleFail()
    }
    return
  }
  // success가 아닌 기타 상태는 실패로 간주
  handleFail()
}

// Event Handlers
const handleStartMatching = async () => {
  if (isStartingMatch.value) return
  
  if (!authStore.isLoggedIn) {
    modals.showLoginRequiredModal()
    return
  }
  
  isStartingMatch.value = true
  
  try {
    await webSocket.connect()
    webSocket.handleMessage(handleWebSocketMessage)
    
    const request = matchingStore.toMatchRequest
    const requestKey = JSON.stringify(request)
    if (lastMatchRequestHash.value && lastMatchRequestHash.value === requestKey) {
      // 동일 요청이 진행 중이면 무시
      return
    }
    lastMatchRequestHash.value = requestKey
    webSocket.sendMatchRequest(request)
    
    matchingStore.startMatching()
    // 타임아웃 모달 미사용: 콜백 없이 타이머 시작
    startMatchingTimer()
    // 시작 완료로 간주: 시작 락 해제 (isMatching이 비활성화 역할 지속)
    isStartingMatch.value = false
    lastMatchRequestHash.value = null
  } catch (error) {
    console.error('❌ 매칭 시작 실패:', error)
    actions.handleError('매칭 시작에 실패했습니다.')
    isStartingMatch.value = false
    lastMatchRequestHash.value = null
  }
}

const handleCancelMatching = () => {
  matchingStore.cancelMatching()
  stopMatchingTimer()
  webSocket.disconnect()
}

const handleModalAccept = () => {
  const matchId = matchingStore.currentMatchId
  const teamNumber = matchingStore.getCurrentUserTeam()
  
  if (!matchId) return
  // 동일 matchId로 중복 전송 차단
  if (sentAcceptanceForMatchIds.has(matchId)) return
  sentAcceptanceForMatchIds.add(matchId)

  if (matchId) {
    webSocket.sendMatchAcceptance(matchId, true, teamNumber)
  }
  
  selfAcceptance.value = 'accepted'
  matchingStore.stopAcceptTimer()
  stopAcceptTimer()

  // 수락 시 즉시 'waiting' 화면으로 전환하여 게임 스테이터스 표시
  // (소켓은 유지, 서버 MATCH_RESULT 수신 시 최종 처리)
  if (!matchingStore.isMatching) {
    matchingStore.startMatching()
    // 타임아웃 모달 미사용: 콜백 없이 타이머 시작
    startMatchingTimer()
  }

  // 초대장 상태는 최종 결과 수신 후 정리 (여기서 초기화하면 주제/옵션 정보가 사라짐)
}

const handleModalReject = () => {
  const matchId = matchingStore.currentMatchId
  const teamNumber = matchingStore.getCurrentUserTeam()
  
  if (!matchId) return
  // 동일 matchId로 중복 전송 차단
  if (sentAcceptanceForMatchIds.has(matchId)) return
  sentAcceptanceForMatchIds.add(matchId)

  if (matchId) {
    webSocket.sendMatchAcceptance(matchId, false, teamNumber)
  }
  
  selfAcceptance.value = 'rejected'
  matchingStore.stopAcceptTimer()
  stopAcceptTimer()
  matchingStore.acceptTimeLeft = 0
}

const handleMatchSuccess = (roomId: string) => {
  matchingStore.setMatchResult({ 
    roomId,
    topicId: (currentMatchTopicId.value ?? 0),
    stance: currentUserStance.value,
    mode: currentMatchMode.value,
    participants: []
  })
  
  matchingStore.setStatus('completed')
  // 전송 락 해제
  if (matchingStore.currentMatchId) {
    sentAcceptanceForMatchIds.delete(matchingStore.currentMatchId)
  }
}

// 타임아웃/주제변경(아워워닝/토픽체인지) 미사용으로 관련 핸들러 제거

// Watchers
watch(
  () => matchingStore.status,
  (newStatus) => {
    if (newStatus === 'completed' && matchingStore.matchResult?.roomId) {
      const roomId = matchingStore.matchResult.roomId
      
      // 매칭 관련 상태 초기화
      handleCancelMatching()
      modals.hideAllModals()
      
      // 토론방으로 라우팅
      router.push(`/debate/${roomId}`)
      
      // 라우팅 후 스토어 리셋
      setTimeout(() => matchingStore.reset(), 100)
    }
  }
)

// 에러 토스트 자동 해제 (3초)
watch(
  () => matchingStore.error,
  (err) => {
    if (err) {
      setTimeout(() => matchingStore.clearError(), 3000)
    }
  }
)

// Lifecycle
onMounted(async () => {
  // 진입 시 매칭 상태만 초기화 (토픽 fetch/타이머는 useTopicSetController에서 관리)
  matchingStore.cancelMatching()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  
  webSocket.disconnect()
  matchingStore.reset()
  modals.hideAllModals()
})

// Timer placeholder (legacy). 주제 변경 타이머는 useTopicSetController에서 일원화됨
let timer: ReturnType<typeof setInterval> | null = null
</script>