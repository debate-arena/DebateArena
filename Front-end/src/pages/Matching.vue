<template>
  <div class="min-h-screen bg-background">
    <!-- 헤더 -->
    <header class="border-b border-border bg-card">
      <div class="container mx-auto px-6 py-4">
        <div class="flex items-center justify-between">
          <h1 class="text-xl font-semibold text-foreground">매칭</h1>
          <Button variant="outline" @click="$router.push('/')">
            홈으로
          </Button>
        </div>
      </div>
    </header>

    <!-- 메인 콘텐츠 -->
    <div class="container mx-auto px-6 py-8">
      <div class="flex gap-8">
        <!-- 좌측: 매칭 선택 영역 -->
        <div class="flex-1 space-y-6">
          <!-- 주제 변경까지 남은 시간 -->
          <Card class="p-4">
            <div class="text-center">
              <p class="text-sm text-muted-foreground">주제 변경까지 남은 시간</p>
              <p class="text-2xl font-mono text-foreground">{{ formatTime(remainingTime) }}</p>
            </div>
          </Card>

          <!-- 글로벌 선택 -->
          <PlayerCountSelection />

          <!-- 주제 카드들 -->
          <div class="space-y-4">
            <TopicCard 
              v-for="topic in topicSetStore.currentSet?.topics || []" 
              :key="topic.id"
              :topic="topic"
            />
          </div>
        </div>

        <!-- 우측: 매칭 시작(요약/타이머) 영역 -->
        <div class="w-80 space-y-6">
          <!-- 매칭 시작 버튼 -->
          <Card class="p-4">
            <div class="relative">
              <Button 
                @click="handleStartMatching"
                :disabled="!matchingStore.canStartMatching || matchingStore.isMatching"
                class="w-full"
                size="lg"
              >
                <span v-if="!matchingStore.isMatching">매칭 시작</span>
                <span v-else class="flex items-center gap-2">
                  <div class="animate-spin rounded-full h-4 w-4 border-2 border-current border-t-transparent"></div>
                  매칭 대기 중...
                </span>
              </Button>
              
              <!-- 취소 버튼 (매칭 중일 때만 표시) -->
              <Button
                v-if="matchingStore.isMatching"
                @click="handleCancelMatching"
                variant="destructive"
                size="sm"
                class="absolute -top-2 -right-2 h-6 w-6 rounded-full p-0 shadow-lg"
              >
                <span class="text-xs">×</span>
              </Button>
            </div>
          </Card>
          
          <!-- 조건 안내 -->
          <div v-if="!matchingStore.canStartMatching" class="text-sm text-muted-foreground text-center">
            주제와 모드를 각각 최소 1개씩 선택해주세요
          </div>

          <!-- 타이머 & 예상 시간 -->
          <Card class="p-4">
            <div class="space-y-4">
              <div class="space-y-2">
                <h3 class="text-sm font-medium text-foreground">타이머</h3>
                <p class="text-2xl font-mono text-foreground">
                  {{ formatTime(matchingStore.elapsedTime) }}
                </p>
              </div>
              <div v-if="matchingStore.isMatching" class="space-y-2">
                <h3 class="text-sm font-medium text-foreground">예상 시간</h3>
                <p class="text-sm text-muted-foreground">
                  {{ getEstimatedTime() }}
                </p>
              </div>
            </div>
          </Card>

          <!-- 내 선택 목록 -->
          <Card class="p-4">
            <div class="space-y-2">
              <h3 class="text-sm font-medium text-foreground">내 선택 목록</h3>
              <div v-if="matchingStore.selectedTopicSelections.length === 0" class="text-sm text-muted-foreground">
                선택된 주제가 없습니다
              </div>
              <div v-else class="space-y-2">
                <div 
                  v-for="(item, index) in getSortedSelectedItems" 
                  :key="index"
                  class="text-sm"
                >
                  <div class="text-foreground">
                    {{ index + 1 }}) {{ getTopicTitle(item.topicId) }}: {{ getStanceText(item.topicId, item.stance) }}
                  </div>
                  <div class="text-muted-foreground ml-4">
                    모드: {{ item.modes.join(', ') }}
                  </div>
                </div>
              </div>
            </div>
          </Card>

          <!-- 규칙/안내 -->
          <Card class="p-4">
            <div class="space-y-2">
              <h3 class="text-sm font-medium text-foreground">규칙/안내</h3>
              <ul class="text-xs text-muted-foreground space-y-1">
                <li>• 주제는 여러 개 선택 가능</li>
                <li>• 각 주제 내 진영은 단일 선택(찬성/반대/랜덤 中 1)</li>
                <li>• 취소 시 전체 선택 초기화</li>
              </ul>
            </div>
          </Card>
        </div>
      </div>
    </div>
  </div>

  <!-- 모달들 -->
  <!-- 매칭 시작 확인 모달 -->
  <Dialog v-model:open="isStartModalOpen" @update:open="handleStartModalClose">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center">매칭 시작</DialogTitle>
        <DialogDescription class="text-center">
          선택한 주제와 모드로 매칭을 시작하시겠습니까?
        </DialogDescription>
      </DialogHeader>
      
      <div class="space-y-4">
        <div class="text-center space-y-2">
          <p class="text-sm text-muted-foreground">
            선택된 주제: {{ matchingStore.selectedTopicCount }}개
          </p>
          <p class="text-sm text-muted-foreground">
            선택된 모드: {{ getModeSummary() }}
          </p>
        </div>
        
        <div class="space-y-2">
          <Button size="lg" class="w-full" @click="confirmStartMatching">매칭 시작</Button>
          <Button variant="outline" size="lg" class="w-full" @click="cancelStartMatching">취소</Button>
        </div>
      </div>
    </DialogContent>
  </Dialog>

  <!-- 매칭 성사 모달 -->
  <Dialog v-model:open="isMatchCompleteModalOpen" @update:open="handleMatchCompleteModalClose">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center">🎉 매칭 성사!</DialogTitle>
        <DialogDescription class="text-center">
          매칭이 성사되었습니다.
        </DialogDescription>
      </DialogHeader>
      
      <div class="space-y-4">
        <div class="text-center space-y-2">
          <p class="text-sm text-muted-foreground">
            주제: {{ getTopicTitle(matchInfo.topicId) }}
          </p>
          <p class="text-sm text-muted-foreground">
            진영: {{ getStanceText(matchInfo.topicId, matchInfo.stance) }}
          </p>
          <p class="text-sm text-muted-foreground">
            모드: {{ matchInfo.mode }}
          </p>
        </div>
        
        <div class="space-y-2">
          <Button size="lg" class="w-full" @click="acceptMatch">수락</Button>
          <Button variant="outline" size="sm" class="w-full" @click="cancelAllMatches">모든 매칭 취소</Button>
        </div>
      </div>
    </DialogContent>
  </Dialog>

  <!-- 연결 중 모달 -->
  <Dialog v-model:open="isConnectingModalOpen" @update:open="handleConnectingModalClose">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center">🔗 연결 중...</DialogTitle>
        <DialogDescription class="text-center">
          토론방에 연결하고 있습니다.
        </DialogDescription>
      </DialogHeader>
      
      <div class="space-y-4">
        <div class="text-center space-y-2">
          <p class="text-sm text-muted-foreground">
            방 ID: {{ roomInfo.roomId }}
          </p>
          <p class="text-sm text-muted-foreground">
            연결된 사용자: {{ roomInfo.connectedUsers }}/{{ roomInfo.totalUsers }}
            {{ console.log('모달 렌더링:', roomInfo.connectedUsers) }}
          </p>
        </div>
        
        <!-- 연결 상태 표시 -->
        <div class="flex justify-center gap-2">
          <UserIcon
            v-for="index in roomInfo.totalUsers"
            :key="index"
            :class="getIconClass(index) + ' text-2xl'"
          />
        </div>
      </div>
    </DialogContent>
  </Dialog>

  <!-- 타임아웃 모달 -->
  <Dialog v-model:open="isTimeoutModalOpen" @update:open="handleTimeoutModalClose">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center">⏰ 매칭 타임아웃</DialogTitle>
        <DialogDescription class="text-center">
          매칭 시간이 초과되었습니다.
        </DialogDescription>
      </DialogHeader>
      
      <div class="space-y-4">
        <div class="text-center space-y-2">
          <p class="text-sm text-muted-foreground">
            다시 매칭을 시도하시겠습니까?
          </p>
        </div>
        
        <div class="space-y-2">
          <Button size="lg" class="w-full" @click="restartMatching">다시 매칭하기</Button>
          <Button variant="outline" size="lg" class="w-full" @click="handleTimeoutModalClose">취소</Button>
        </div>
      </div>
    </DialogContent>
  </Dialog>

  <!-- 주제 변경으로 취소 알림 모달 -->
  <Dialog v-model:open="isTopicChangeModalOpen" @update:open="handleTopicChangeModalClose">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center">
          ⏰ 주제 변경으로 인한 매칭 취소
        </DialogTitle>
        <DialogDescription class="text-center">
          주제가 변경되어 매칭이 자동으로 취소되었습니다.
        </DialogDescription>
      </DialogHeader>
      
      <div class="space-y-4">
        <div class="text-center space-y-2">
          <p class="text-sm text-muted-foreground">
            새로운 주제로 다시 매칭하시겠습니까?
          </p>
        </div>
        
        <div class="space-y-2">
          <Button 
            variant="outline" 
            size="lg" 
            class="w-full" 
            @click="handleTopicChangeModalClose"
          >
            취소
          </Button>
        </div>
      </div>
    </DialogContent>
  </Dialog>

  <!-- 주제 변경 경고 모달 -->
  <Dialog v-model:open="isHourWarningModalOpen" @update:open="handleHourWarningModalClose">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center">
          ⚠️ 주제 변경 예정
        </DialogTitle>
        <DialogDescription class="text-center">
          주제가 곧 변경됩니다.
        </DialogDescription>
      </DialogHeader>
      
      <div class="space-y-4">
        <div class="text-center space-y-2">
          <p class="text-sm text-muted-foreground">
            주제 변경 후 10초가 지나면 모든 매칭이 자동으로 취소됩니다.
          </p>
          <p class="text-sm font-medium text-foreground">
            계속 진행하시겠습니까?
          </p>
        </div>
        
        <div class="space-y-2">
          <Button size="lg" class="w-full" @click="confirmHourWarning">계속 진행</Button>
          <Button variant="outline" size="lg" class="w-full" @click="hideHourWarningModal">취소</Button>
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { useMatchingStore } from '@/store/matching'
import { useTopicSetStore } from '@/store/topicSet'
import { useMatchingModals } from '@/composables/useMatchingModals'
import { useMatchingTimer } from '@/composables/useMatchingTimer'
import { useMatchingActions } from '@/composables/useMatchingActions'
import { useWebSocket } from '@/composables/useWebSocket'
import { formatEstimatedTime } from '@/utils/matching'
import PlayerCountSelection from '@/components/matching/PlayerCountSelection.vue'
import TopicCard from '@/components/matching/TopicCard.vue'
import type { Stance, PlayerMode } from '@/types/matching'
import { UserIcon } from 'lucide-vue-next'

const router = useRouter()
const matchingStore = useMatchingStore()
const topicSetStore = useTopicSetStore()

// 타이머 관리
const { startMatchingTimer, stopMatchingTimer } = useMatchingTimer()

// 타이머 관련 함수들
const remainingTime = ref(0)
let timer: ReturnType<typeof setInterval> | null = null
const formatTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 웹소켓 연결
const connectWebSocket = () => {
  console.log('🔌 WebSocket 연결 시도')
}
const disconnectWebSocket = () => {
  console.log('🔌 WebSocket 연결 해제')
}

// 모달 관리
const {
  isStartModalOpen,
  isMatchCompleteModalOpen,
  isTimeoutModalOpen,
  isConnectingModalOpen,
  isTopicChangeModalOpen,
  isHourWarningModalOpen,
  showStartModal,
  hideStartModal,
  showMatchCompleteModal,
  hideMatchCompleteModal,
  showTimeoutModal,
  hideTimeoutModal,
  showConnectingModal,
  hideConnectingModal,
  showTopicChangeModal,
  hideTopicChangeModal,
  showHourWarningModal,
  hideHourWarningModal
} = useMatchingModals()

// 매칭 액션 관리
const { startMatching, cancelMatching } = useMatchingActions()

// 연결 상태 관리
const roomInfo = ref({
  roomId: '',
  totalUsers: 0,
  connectedUsers: 0
})

let connectionInterval: ReturnType<typeof setInterval> | null = null

// 연결 진행 시뮬레이션
const simulateConnection = () => {
  console.log('simulateConnection 시작:', JSON.stringify(roomInfo.value))
  roomInfo.value = {
    roomId: 'ROOM_' + Math.random().toString(36).substr(2, 9),
    totalUsers: 4,
    connectedUsers: 0
  }
  
  connectionInterval = setInterval(() => {
    if (roomInfo.value.connectedUsers < roomInfo.value.totalUsers) {
      roomInfo.value.connectedUsers++
      console.log('연결 인원 증가:', roomInfo.value.connectedUsers)
    } else {
      if (connectionInterval) {
        clearInterval(connectionInterval)
        connectionInterval = null
      }
      hideConnectingModal()
      router.push('/debate')
    }
  }, 1000)
}

// 연결 상태 가져오기
const getConnectionStatus = () => {
  const totalUsers = roomInfo.value.totalUsers
  const connectedUsers = roomInfo.value.connectedUsers
  const status = []
  
  for (let i = 0; i < totalUsers; i++) {
    status.push(i < connectedUsers)
  }
  
  return status
}

// 정렬된 선택 목록 (주제 ID 순, 모드는 1:1이 앞에 오도록)
const getSortedSelectedItems = computed(() => {
  return matchingStore.selectedTopicSelections
    .sort((a, b) => {
      // 주제 ID로만 정렬 (모드는 이미 modeOrder로 관리됨)
      return a.topicId - b.topicId
    })
})

// 예상 대기 시간
const getEstimatedTime = () => {
  if (!matchingStore.isMatching) return '매칭 중...'
  return formatEstimatedTime(Math.floor(matchingStore.elapsedTime / 60))
}

// 주제 제목 가져오기
const getTopicTitle = (topicId: number) => {
  const topic = matchingStore.getTopicById(topicId)
  return topic?.title || `주제 ${topicId}`
}

// 진영 텍스트 변환
const getStanceText = (topicId: number, stance: Stance) => {
  const topic = matchingStore.getTopicById(topicId)
  if (!topic) return '알 수 없음'
  
  switch (stance) {
    case 'option1': return topic.option1
    case 'option2': return topic.option2
    case 'random': return '상관없음'
    default: return '알 수 없음'
  }
}

// 모드 요약
const getModeSummary = () => {
  const modes = Array.from(matchingStore.globalModes)
  return modes.length > 0 ? modes.join(', ') : '미선택'
}

// 매칭 시작 처리
const handleStartMatching = () => {
  // 정각 5분 전(300초)인지 체크
  if (remainingTime.value <= 300) {
    showHourWarningModal()
  } else {
    // 바로 매칭 시작
    const selections = matchingStore.selectedTopicSelections
    startMatching(selections, handleTimeout)
    startMatchingTimer(handleTimeout)
    connectWebSocket()
  }
}

// 매칭 취소 처리
const handleCancelMatching = () => {
  cancelMatching(stopMatchingTimer)
  hideStartModal()
  hideHourWarningModal()
  
  // WebSocket 연결 해제
  disconnectWebSocket()
}

// 매칭 성사 처리
const handleMatchSuccess = (result: any) => {
  // This function is no longer used as per the new_code, but keeping it for now
  // as it might be called from elsewhere or for future use.
  // The new_code calls simulateConnection directly.
}

// 타임아웃 처리
const handleTimeout = () => {
  showTimeoutModal()
}

// 매칭 수락
const acceptMatch = () => {
  hideMatchCompleteModal()
  showConnectingModal()
  simulateConnection()
}

// 모든 매칭 취소
const cancelAllMatches = () => {
  hideMatchCompleteModal()
  hideConnectingModal()
  if (connectionInterval) {
    clearInterval(connectionInterval)
    connectionInterval = null
  }
  roomInfo.value = { roomId: '', totalUsers: 0, connectedUsers: 0 }
  cancelMatching(stopMatchingTimer)
}

// 연결 모달 닫기
const handleConnectingModalClose = () => {
  hideConnectingModal()
  matchingStore.cancelMatching()
  stopMatchingTimer()
  disconnectWebSocket()
  if (connectionInterval) {
    clearInterval(connectionInterval)
    connectionInterval = null
  }
  roomInfo.value = { roomId: '', totalUsers: 0, connectedUsers: 0 }
}

// 주제 변경 경고 모달 닫기
const handleHourWarningModalClose = () => {
  hideHourWarningModal()
  showStartModal()
}

function resetMatchingToInitial() {
  // 모든 주제 id 추출
  const topicIds = (topicSetStore.currentSet?.topics || []).map(t => t.id)
  // 글로벌 상태 초기화
  matchingStore.globalModes = new Set(['1:1', '2:2'])
  matchingStore.globalStances = new Set(['random'])
  // 각 주제도 동일하게 초기화
  matchingStore.initializeTopicSelections(topicIds)
}

const handleTopicChangeModalClose = () => {
  console.log('handleTopicChangeModalClose 호출됨')
  hideTopicChangeModal()
  matchingStore.cancelMatching()
  matchingStore.isMatching = false
  matchingStore.status = 'idle'
  matchingStore.elapsedTime = 0
  matchingStore.estimatedWaitTime = undefined
  matchingStore.matchResult = undefined
  matchingStore.error = undefined
  matchingStore.clearAllSelections()
  stopMatchingTimer()
  disconnectWebSocket()
  if (connectionInterval) {
    clearInterval(connectionInterval)
    connectionInterval = null
  }
  roomInfo.value = { roomId: '', totalUsers: 0, connectedUsers: 0 }
  resetMatchingToInitial()
}

// 페이지 진입 시 초기화
onMounted(async () => {
  console.log('📱 Matching 페이지 진입 - 초기화 시작')
  
  // 매칭 상태 초기화
  matchingStore.cancelMatching()
  stopMatchingTimer()
  disconnectWebSocket()
  if (connectionInterval) {
    clearInterval(connectionInterval)
    connectionInterval = null
  }
  roomInfo.value = { roomId: '', totalUsers: 0, connectedUsers: 0 }
  
  // 주제 정보 가져오기 후 초기화
  await topicSetStore.fetchTopicSets()
  const activeTopics = topicSetStore.currentSet?.topics || []
  console.log('📱 활성 주제:', activeTopics)
  
  // 글로벌 상태를 먼저 설정
  matchingStore.globalModes = new Set(['1:1', '2:2'])
  matchingStore.globalStances = new Set(['random'])
  
  // 주제별 초기 선택 생성 (글로벌 상태가 이미 설정된 상태에서)
  matchingStore.initializeTopicSelections(activeTopics.map(topic => topic.id))
  
  // DOM 업데이트를 강제로 트리거
  await nextTick()
  
  console.log('📱 Matching 페이지 초기화 완료:', {
    globalModes: Array.from(matchingStore.globalModes),
    globalStances: Array.from(matchingStore.globalStances),
    topicSelections: matchingStore.topicSelections
  })
  
  // 웹소켓 연결 시도
  connectWebSocket()
})

// Watch for currentSet changes to update remainingTime
watch(
  () => topicSetStore.currentSet,
  (currentSet) => {
    if (timer) clearInterval(timer)
    if (currentSet && currentSet.endAtMs) {
      const update = () => {
        const now = Date.now()
        const end = currentSet.endAtMs
        const diff = end - now
        const seconds = Math.max(0, Math.floor(diff / 1000))
        remainingTime.value = seconds
      }
      update()
      timer = setInterval(update, 1000)
    } else {
      remainingTime.value = 0
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  if (timer) clearInterval(timer)
  cancelMatching(stopMatchingTimer)
  disconnectWebSocket()
  if (connectionInterval) {
    clearInterval(connectionInterval)
  }
})

// 디버깅용 watch
watch(isMatchCompleteModalOpen, (newVal) => {
  console.log('🔍 isMatchCompleteModalOpen changed:', newVal)
})

watch(isTimeoutModalOpen, (newVal) => {
  console.log('🔍 isTimeoutModalOpen changed:', newVal)
})

watch(isHourWarningModalOpen, (newVal) => {
  console.log('🔍 isHourWarningModalOpen changed:', newVal)
})

// 매칭 시작 취소
const cancelStartMatching = () => {
  hideStartModal()
}

// 매칭 시작 확인
const confirmStartMatching = () => {
  hideStartModal()
  // 실제 매칭 시작 로직은 handleStartMatching에서 처리
}

// 주제 변경 경고 확인
const confirmHourWarning = () => {
  hideHourWarningModal()
  const selections = matchingStore.selectedTopicSelections
  startMatching(selections, handleTimeout)
  startMatchingTimer(handleTimeout)
  connectWebSocket()
}

// 매칭 시작 모달 닫기 핸들러
const handleStartModalClose = (isOpen: boolean) => {
  if (!isOpen) {
    hideStartModal()
  }
}

// 매칭 완료 모달 닫기 핸들러
const handleMatchCompleteModalClose = () => {
  hideMatchCompleteModal()
  matchingStore.cancelMatching()
  stopMatchingTimer()
  disconnectWebSocket()
  if (connectionInterval) {
    clearInterval(connectionInterval)
    connectionInterval = null
  }
  roomInfo.value = { roomId: '', totalUsers: 0, connectedUsers: 0 }
}

// 타임아웃 모달 닫기 핸들러
const handleTimeoutModalClose = () => {
  hideTimeoutModal()
  matchingStore.cancelMatching()
  stopMatchingTimer()
  disconnectWebSocket()
  if (connectionInterval) {
    clearInterval(connectionInterval)
    connectionInterval = null
  }
  roomInfo.value = { roomId: '', totalUsers: 0, connectedUsers: 0 }
}

// 다시 매칭하기
const restartMatching = () => {
  hideTimeoutModal()
  handleStartMatching()
}

// 매칭 정보 (임시)
const matchInfo = ref({
  topicId: 1,
  stance: 'option1' as Stance,
  mode: '1:1' as PlayerMode,
  topicTitle: 'AI 규제는 필요한가?',
  myStance: '찬성'
})

const getIconClass = (index: number) => {
  // index는 1부터 시작하므로 0부터 시작하는 인덱스로 변환
  const iconIndex = index - 1
  const active = iconIndex < roomInfo.value.connectedUsers
  const cls = active ? 'text-green-500' : 'text-gray-400'
  console.log(`아이콘 ${index} (인덱스 ${iconIndex}): connectedUsers=${roomInfo.value.connectedUsers}, active=${active}, class=${cls}`)
  return cls
}
</script> 