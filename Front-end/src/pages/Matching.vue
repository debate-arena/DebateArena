<template>
  <div class="min-h-screen bg-background">
    <!-- 에러 메시지 표시 -->
    <div v-if="matchingStore.error" class="fixed inset-0 flex items-center justify-center z-50">
      <Alert class="max-w-md bg-background shadow-lg">
        <AlertCircle class="h-4 w-4" />
        <AlertTitle>알림</AlertTitle>
        <AlertDescription>{{ matchingStore.error }}</AlertDescription>
      </Alert>
    </div>

    <!-- 메인 콘텐츠 -->
    <div class="max-w-6xl mx-auto px-6 py-8">
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
        :topic-id="currentMatchTopicId"
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
    
    <!-- 모달들 -->
    <TimeoutModal 
      v-model:open="modals.modalState.value.isTimeoutModalOpen" 
      @close="handleTimeoutModalClose" 
    />
    
    <TopicChangeModal 
      v-model:open="modals.modalState.value.isTopicChangeModalOpen" 
      @close="handleTopicChangeModalClose" 
    />
    
    <HourWarningModal 
      v-model:open="modals.modalState.value.isHourWarningModalOpen" 
      @confirm="confirmHourWarning"
      @close="modals.hideHourWarningModal()"
    />
    
    <LoginRequiredModal 
      v-model:open="modals.modalState.value.isLoginRequiredModalOpen" 
      @close="modals.hideLoginRequiredModal()" 
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
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
import { AlertCircle } from 'lucide-vue-next'

// Components
import MatchingSelectionArea from '@/components/matching/MatchingSelectionArea.vue'
import MatchResultPanel from '@/components/matching/MatchResultPanel.vue'
import GameStatsPanel from '@/components/matching/GameStatsPanel.vue'
import TimeoutModal from '@/components/matching/TimeoutModal.vue'
import TopicChangeModal from '@/components/matching/TopicChangeModal.vue'
import HourWarningModal from '@/components/matching/HourWarningModal.vue'
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

// Controllers
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

// Computed Properties
const currentMatchTopicId = computed(() => matchingStore.currentMatchTopicId || 0)
const currentMatchMode = computed(() => matchingStore.currentMatchMode || '1:1')
const currentUserStance = computed(() => teamToStanceFlexible(matchingStore.currentUserTeam))
const currentMatchTopicTitle = computed(() => {
  const topic = topicSetStore.currentSet?.topics.find(t => t.id === currentMatchTopicId.value)
  return topic?.title || `주제 ${currentMatchTopicId.value}`
})
const currentMatchTotalCount = computed(() => getTotalCount(currentMatchMode.value))

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
    selfAcceptance.value = 'pending'
    
    // 수락 타이머 시작
    matchingStore.startAcceptTimer()
    startAcceptTimer()
  }
}

const handleAcceptanceStatus = (data: WebSocketMessage) => {
  if (data.status === 'error') return
  
  if (data.status === 'success') {
    const { accept, team, stance } = processAcceptanceStatus(data)
    
    matchingStore.updateRoomInfo({ 
      connectedUsers: matchingStore.roomInfo.connectedUsers + 1 
    })
    
    if (accept) {
      matchingStore.setLastAcceptedStance(stance)
    }
    
    matchResultState.updateStanceAcceptance(stance, accept)
  }
}

const handleMatchResult = (data: WebSocketMessage) => {
  if (data.status === 'error') return
  
  if (data.status === 'success') {
    const result = processMatchResult(data)
    
    if (result.success && result.roomId) {
      handleMatchSuccess(result.roomId)
    }
  }
}

// Event Handlers
const handleStartMatching = async () => {
  if (isStartingMatch.value) return
  
  if (!authStore.isLoggedIn) {
    modals.showLoginRequiredModal()
    return
  }
  
  if (topicSetStore.remainingTimeSeconds <= 300) {
    modals.showHourWarningModal()
    return
  }
  
  isStartingMatch.value = true
  
  try {
    await webSocket.connect()
    webSocket.handleMessage(handleWebSocketMessage)
    
    const request = matchingStore.toMatchRequest
    webSocket.sendMatchRequest(request)
    
    matchingStore.startMatching()
    startMatchingTimer(() => modals.showTimeoutModal())
  } catch (error) {
    console.error('❌ 매칭 시작 실패:', error)
    actions.handleError('매칭 시작에 실패했습니다.')
  } finally {
    setTimeout(() => { isStartingMatch.value = false }, 1000)
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
  
  if (matchId) {
    webSocket.sendMatchAcceptance(matchId, true, teamNumber)
  }
  
  selfAcceptance.value = 'accepted'
  matchingStore.stopAcceptTimer()
  stopAcceptTimer()
}

const handleModalReject = () => {
  const matchId = matchingStore.currentMatchId
  const teamNumber = matchingStore.getCurrentUserTeam()
  
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
    topicId: currentMatchTopicId.value,
    stance: currentUserStance.value,
    mode: currentMatchMode.value,
    participants: []
  })
  
  matchingStore.setStatus('completed')
}

const handleTimeoutModalClose = () => {
  modals.hideTimeoutModal()
  handleCancelMatching()
}

const handleTopicChangeModalClose = () => {
  modals.hideTopicChangeModal()
}

const confirmHourWarning = async () => {
  modals.hideHourWarningModal()
  
  if (!authStore.isLoggedIn) {
    modals.showLoginRequiredModal()
    return
  }
  
  isStartingMatch.value = true
  
  try {
    await webSocket.connect()
    webSocket.handleMessage(handleWebSocketMessage)
    
    const request = matchingStore.toMatchRequest
    webSocket.sendMatchRequest(request)
    
    matchingStore.startMatching()
    startMatchingTimer(() => modals.showTimeoutModal())
  } catch (error) {
    console.error('❌ 매칭 시작 실패:', error)
    actions.handleError('매칭 시작에 실패했습니다.')
  } finally {
    setTimeout(() => { isStartingMatch.value = false }, 1000)
  }
}

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
      router.push(`/debate-room/${roomId}`)
      
      // 라우팅 후 스토어 리셋
      setTimeout(() => matchingStore.reset(), 100)
    }
  }
)

// Lifecycle
onMounted(async () => {
  matchingStore.cancelMatching()
  await topicSetStore.fetchTopicSets()
  
  const activeTopics = topicSetStore.currentSet?.topics || []
  matchingStore.globalModes = new Set(['1:1', '2:2'])
  matchingStore.globalStances = new Set(['random'])
  matchingStore.initializeTopicSelections(activeTopics.map(topic => topic.id))
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

// Timer (for topic change)
let timer: ReturnType<typeof setInterval> | null = null

const startTopicChangeTimer = () => {
  if (topicSetStore.currentSet?.remainingTimeSeconds) {
    const update = () => {
      const remainingSeconds = topicSetStore.remainingTimeSeconds
      if (remainingSeconds <= 0) {
        topicSetStore.swapSets()
      }
    }
    update()
    timer = setInterval(update, 1000)
  }
}

onMounted(() => {
  startTopicChangeTimer()
})
</script>