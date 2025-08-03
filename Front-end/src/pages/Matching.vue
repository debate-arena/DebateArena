<template>
  <div class="min-h-screen bg-background">

    <!-- 테스트 버튼 (임시) -->
    <div class="fixed top-4 right-4 flex flex-col gap-2 z-50">
      <Button @click="showTestMatchModal" size="sm" variant="outline" class="text-xs">
        매칭 성사 테스트
      </Button>
    </div>

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
              <div v-else class="space-y-3">
                <Card 
                  v-for="selection in matchingStore.selectedTopicSelections" 
                  :key="`${selection.topicId}-${selection.stance}`"
                  class="p-3"
                >
                  <div class="space-y-2">
                    <div class="font-medium text-foreground">{{ getTopicTitle(selection.topicId) }}</div>
                    <div class="text-sm space-y-2">
                      <div class="flex items-center gap-2">
                        <span class="font-medium text-muted-foreground">선택:</span>
                        <Button 
                          size="sm" 
                          variant="outline"
                          class="h-6 px-2 text-xs"
                          :class="getStanceButtonClass(selection.stance)"
                        >
                          {{ getStanceText(selection.topicId, selection.stance) }}
                        </Button>
                      </div>
                      <div class="flex items-center gap-2">
                        <span class="font-medium text-muted-foreground">모드:</span>
                        <div class="flex gap-1">
                          <Button 
                            v-for="mode in sortModes(selection.modes)" 
                            :key="mode"
                            size="sm" 
                            variant="outline"
                            class="h-6 px-2 text-xs"
                            :class="getModeButtonClass(mode)"
                          >
                            {{ mode }}
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                </Card>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>

    <!-- 모달들 -->
    <!-- 매칭 성사 모달 -->
    <MatchingModal
      :is-open="modals.modalState.value.isMatchCompleteModalOpen"
      :topic-title="modals.matchModalData.value.topicTitle"
      :stance-text="modals.matchModalData.value.stanceText"
      :mode="modals.matchModalData.value.mode"
      :topic-id="modals.matchModalData.value.topicId"
      :users="Array.from(modals.matchUsers.value.values())"
      :connected-count="modals.connectedCount.value"
      :total-count="modals.totalCount.value"
      :time-left="matchingState.acceptTimeLeft.value"
      :is-connecting="matchingState.isConnectingActive.value"
      @accept="handleModalAccept"
      @reject="handleModalReject"
    />
    
    <!-- 타임아웃 모달 -->
    <Dialog v-model:open="modals.modalState.value.isTimeoutModalOpen" @update:open="handleTimeoutModalClose">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle class="text-center">⏰ 매칭 타임아웃</DialogTitle>
          <DialogDescription class="text-center">
            매칭 시간이 초과되었습니다. 다시 시도해주세요.
          </DialogDescription>
        </DialogHeader>
        <div class="flex gap-2">
          <Button @click="modals.hideTimeoutModal()" class="flex-1">
            확인
          </Button>
        </div>
      </DialogContent>
    </Dialog>

    <!-- 주제 변경으로 취소 알림 모달 -->
    <Dialog v-model:open="modals.modalState.value.isTopicChangeModalOpen" @update:open="handleTopicChangeModalClose">
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
    <Dialog v-model:open="modals.modalState.value.isHourWarningModalOpen" @update:open="handleHourWarningModalClose">
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
              현재 주제로 매칭을 시작하시겠습니까?
            </p>
          </div>
          
          <div class="space-y-2">
            <Button size="lg" class="w-full" @click="confirmHourWarning">계속 진행</Button>
            <Button variant="outline" size="lg" class="w-full" @click="modals.hideHourWarningModal()">취소</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { useMatchingStore } from '@/store/matching'
import { useTopicSetStore } from '@/store/topicSet'
import { useMatchingTimer } from '@/composables/useMatchingTimer'
import { useMatchingActions } from '@/composables/useMatchingActions'
import { useMatchingState } from '@/composables/useMatchingState'
import { useMatchingModals } from '@/composables/useMatchingModals'
import { useWebSocket } from '@/composables/useWebSocket'
import MatchingModal from '@/components/matching/MatchingModal.vue'
import { formatEstimatedTime, sortModes } from '@/utils/matching'
import PlayerCountSelection from '@/components/matching/PlayerCountSelection.vue'
import TopicCard from '@/components/matching/TopicCard.vue'
import type { Stance, PlayerMode } from '@/types/matching'
import { UserIcon } from 'lucide-vue-next'
import { useThemeStore } from '@/store/theme'

// 아이콘 import
import debateLeftIcon from '@/assets/images/profile/debate_left.png'
import debateRightIcon from '@/assets/images/profile/debate_right.png'
import { CheckCircle } from 'lucide-vue-next'

const router = useRouter()
const matchingStore = useMatchingStore()
const topicSetStore = useTopicSetStore()

// 새로운 composable들 사용
const matchingState = useMatchingState()
const modals = useMatchingModals()
const actions = useMatchingActions()
const webSocket = useWebSocket()

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

// WebSocket 메시지 처리
const handleWebSocketMessage = (data: any) => {
  console.log('📨 WebSocket 메시지 처리:', data)
  console.log('🔍 메시지 타입:', data.type)
  console.log('🔍 메시지 데이터:', data.data)
  
  switch (data.type) {
    case 'MATCH_STATUS':
      // 매칭 현황판 처리
      console.log('📊 매칭 현황판:', data.data)
      break
      
    case 'MATCH_INVITATION':
      // 매칭 초대장 처리
      console.log('💌 매칭 초대장 수신됨!')
      console.log('🔍 매칭 초대장 데이터:', data.data)
      
      // 매칭 ID 저장
      matchingState.currentMatchId.value = data.data.data.matchId
      
      // 주제는 첫 번째 주제로 고정
      const invitationFirstTopic = topicSetStore.currentSet?.topics[0]
      const invitationTopicTitle = invitationFirstTopic?.title || '매칭된 주제'
      
      // 모드는 1:1로 고정
      const invitationMode = '1:1'
      
      // 테마에 따라 선택 결정
      const invitationThemeStore = useThemeStore()
      const invitationStance = invitationThemeStore.isDark ? '선택2' : '선택1'
      
      // 매칭 성사 처리
      actions.handleMatchSuccess({
        matchId: data.data.data.matchId,
        topicTitle: invitationTopicTitle,
        stanceText: invitationStance,
        mode: invitationMode,
        topicId: 1
      })
      
      console.log('✅ 매칭 초대장 처리 완료')
      break
      
    case 'MATCH_SUCCESS':
      // 매칭 성사 처리
      console.log('🎉 매칭 성사 수신됨!')
      console.log('🔍 매칭 성사 데이터:', data.data)
      
      // 주제는 첫 번째 주제로 고정
      const successFirstTopic = topicSetStore.currentSet?.topics[0]
      const successTopicTitle = successFirstTopic?.title || '매칭된 주제'
      
      // 모드는 1:1로 고정
      const successMode = '1:1'
      
      // 테마에 따라 선택 결정
      const successThemeStore = useThemeStore()
      const successStance = successThemeStore.isDark ? '선택2' : '선택1'
      
      // 매칭 성사 처리
      actions.handleMatchSuccess({
        matchId: data.data.data.matchId,
        topicTitle: successTopicTitle,
        stanceText: successStance,
        mode: successMode,
        topicId: 1
      })
      
      // 토론방으로 이동
      if (data.data.data.roomUrl) {
        console.log('🔍 토론방으로 이동:', data.data.data.roomUrl)
        router.push(data.data.data.roomUrl)
      } else {
        console.log('🔍 기본 토론방으로 이동: /debate')
        router.push('/debate')
      }
      break
      
    case 'MATCH_FAILURE':
      // 매칭 실패 처리
      console.log('❌ 매칭 실패 수신됨!')
      console.log('🔍 매칭 실패 데이터:', data.data)
      
      actions.handleError(data.data.data.message || '매칭이 취소되었습니다.')
      break
      
    case 'ACCEPTANCE_STATUS':
      // 다른 사람 응답 현황 처리
      console.log('👥 다른 사람 응답 현황 수신됨!')
      console.log('🔍 응답 현황 데이터:', data.data)
      
      const user = data.data.data.user
      const accept = data.data.data.accept
      
      // 사용자 상태 업데이트
      modals.updateUser(user, { accept })
      
      if (accept) {
        matchingState.updateRoomInfo({ connectedUsers: matchingState.roomInfo.value.connectedUsers + 1 })
      }
      
      console.log(`✅ ${user} ${accept ? '수락' : '거절'} - ${matchingState.roomInfo.value.connectedUsers}/${matchingState.roomInfo.value.totalUsers}`)
      break
      
    case 'ERROR':
      // 에러 처리
      console.error('❌ 매칭 에러 수신됨!')
      console.error('🔍 에러 데이터:', data.data)
      actions.handleError(data.data.message)
      break
      
    default:
      console.log('⚠️ 알 수 없는 메시지 타입:', data.type)
      break
  }
}

// 매칭 시작 처리
const handleStartMatching = async () => {
  // 정각 5분 전(300초)인지 체크
  if (remainingTime.value <= 300) {
    modals.showHourWarningModal()
  } else {
    // 바로 매칭 시작
    console.log('🚀 매칭 시작!')
    matchingStore.startMatching()
    
    // 타이머 시작
    startMatchingTimer(() => {
      console.log('⏰ 매칭 타임아웃')
      modals.showTimeoutModal()
    })
  }
}

// 매칭 취소 처리
const handleCancelMatching = () => {
  console.log('❌ 매칭 취소')
  matchingStore.cancelMatching()
  stopMatchingTimer()
  modals.hideAllModals()
}

// 모달 핸들러들
const handleModalAccept = () => {
  console.log('✅ 매칭 수락 버튼 클릭됨')
  console.log('🔍 현재 matchingState 상태:', {
    isConnecting: matchingState.isConnectingActive.value,
    currentMatchId: matchingState.currentMatchId.value,
    acceptTimeLeft: matchingState.acceptTimeLeft.value
  })
  
  try {
    // 연결 상태로 변경
    matchingState.setConnecting()
    console.log('✅ 연결 상태로 변경됨')
    
    // 서버에 수락 메시지 전송
    const matchId = matchingState.currentMatchId.value
    if (matchId) {
      webSocket.sendMatchAcceptance(matchId, true)
      console.log('✅ 수락 메시지 전송됨')
    } else {
      console.warn('⚠️ matchId가 없음')
    }
    
    // 수락 타이머 시작
    matchingState.startAcceptTimer()
    console.log('✅ 수락 타이머 시작됨')
    
    console.log('✅ 매칭 수락 처리 완료')
  } catch (error) {
    console.error('❌ 매칭 수락 처리 실패:', error)
  }
}

const handleModalReject = () => {
  console.log('❌ 매칭 거절 버튼 클릭됨')
  console.log('🔍 현재 matchingState 상태:', {
    isConnecting: matchingState.isConnectingActive.value,
    currentMatchId: matchingState.currentMatchId.value,
    acceptTimeLeft: matchingState.acceptTimeLeft.value
  })
  
  try {
    // 서버에 거절 메시지 전송
    const matchId = matchingState.currentMatchId.value
    if (matchId) {
      webSocket.sendMatchAcceptance(matchId, false)
      console.log('✅ 거절 메시지 전송됨')
    } else {
      console.warn('⚠️ matchId가 없음')
    }
    
    // 모달 닫기
    modals.hideMatchCompleteModal()
    console.log('✅ 모달 닫기됨')
    
    // 타이머 정지
    matchingState.stopAcceptTimer()
    console.log('✅ 타이머 정지됨')
    
    console.log('❌ 매칭 거절 처리 완료')
  } catch (error) {
    console.error('❌ 매칭 거절 처리 실패:', error)
  }
}

const handleTimeoutModalClose = () => {
  modals.hideTimeoutModal()
  stopMatchingTimer()
  webSocket.disconnect()
  matchingState.reset()
}

const handleTopicChangeModalClose = () => {
  console.log('handleTopicChangeModalClose 호출됨')
  modals.hideTopicChangeModal()
}

const handleHourWarningModalClose = () => {
  modals.hideHourWarningModal()
}

// 주제 변경 경고 확인
const confirmHourWarning = () => {
  modals.hideHourWarningModal()
  handleStartMatching()
}

// 유틸리티 함수들
const getEstimatedTime = () => {
  return formatEstimatedTime(matchingStore.elapsedTime)
}

const getTopicTitle = (topicId: number) => {
  const topic = matchingStore.getTopicById(topicId)
  return topic?.title || '알 수 없는 주제'
}

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

const getStanceButtonClass = (stance: Stance) => {
  switch (stance) {
    case 'option1':
      return 'bg-debate-left hover:bg-debate-left/90 text-slate-800 border-debate-left'
    case 'option2':
      return 'bg-debate-right hover:bg-debate-right/90 text-white border-debate-right'
    case 'random':
      return 'bg-debate-random hover:bg-debate-random/90 text-slate-700 border-debate-random'
    default:
      return 'bg-background hover:bg-accent'
  }
}

const getModeButtonClass = (mode: PlayerMode) => {
  switch (mode) {
    case '1:1':
      return 'bg-mode-1v1 hover:bg-indigo-200 active:bg-indigo-300 text-mode-1v1 border-mode-1v1 ring-mode-1v1'
    case '2:2':
      return 'bg-mode-2v2 hover:bg-mode-2v2/90 text-mode-2v2 border-mode-2v2 ring-mode-2v2'
    default:
      return 'bg-background hover:bg-accent'
  }
}

// 테스트 함수들
const showTestMatchModal = () => {
  console.log('🎯 showTestMatchModal 호출됨')
  
  // 매칭 상태 초기화
  matchingState.reset()
  
  // 매칭 상태를 matched로 설정
  matchingState.setMatched()
  
  // 매칭 ID 설정
  matchingState.currentMatchId.value = 'test-match-id'
  
  // 수락 타이머 시작
  matchingState.startAcceptTimer()
  
  // 모달 데이터 준비
  const modalData = {
    topicTitle: '인공지능의 발전이 인류에게 이익인가, 해악인가?',
    stanceText: '이익이다',
    mode: '2:2',
    topicId: 1
  }
  
  // 테스트용 사용자 데이터
  const testUsers = [
    { userId: '유저1', stance: 'option1', accept: true, timestamp: Date.now() },
    { userId: '유저2', stance: 'option1', accept: null, timestamp: Date.now() },
    { userId: '유저3', stance: 'option2', accept: true, timestamp: Date.now() },
    { userId: '유저4', stance: 'option2', accept: false, timestamp: Date.now() }
  ]
  
  console.log('✅ 테스트 모달 데이터:', modalData)
  console.log('✅ 테스트 사용자 데이터:', testUsers)
  
  // 모달 표시
  modals.showMatchCompleteModal(modalData, testUsers)
  
  console.log('✅ 테스트 모달 표시 완료')
}

// 컴포넌트 마운트/언마운트
onMounted(async () => {
  console.log('🔍 Matching.vue 마운트됨')
  
  // 매칭 상태 초기화
  matchingStore.cancelMatching()
  
  console.log('🔍 초기화 후 매칭 스토어 상태:', {
    isMatching: matchingStore.isMatching,
    status: matchingStore.status,
    elapsedTime: matchingStore.elapsedTime
  })
  
  // 주제 정보 가져오기 후 초기화
  await topicSetStore.fetchTopicSets()
  const activeTopics = topicSetStore.currentSet?.topics || []
  console.log('📱 활성 주제:', activeTopics)
  
  // 글로벌 상태를 기본값으로 설정
  matchingStore.globalModes = new Set(['1:1', '2:2'])
  matchingStore.globalStances = new Set(['random'])
  
  // 주제별 초기 선택 생성
  matchingStore.initializeTopicSelections(activeTopics.map(topic => topic.id))
  
  // DOM 업데이트를 강제로 트리거
  await nextTick()
  
  console.log('📱 Matching 페이지 초기화 완료:', {
    globalModes: Array.from(matchingStore.globalModes),
    globalStances: Array.from(matchingStore.globalStances),
    topicSelections: matchingStore.topicSelections,
    isMatching: matchingStore.isMatching,
    status: matchingStore.status
  })
  
  // WebSocket 메시지 핸들러 설정
  webSocket.handleMessage(handleWebSocketMessage)

  // 타이머 만료 감시
  watch(() => matchingState.matchingState.value.error, (error) => {
    if (error === '수락 시간이 만료되었습니다.') {
      console.log('⏰ 타이머 만료로 모달 닫기')
      modals.hideMatchCompleteModal()
      matchingState.clearError()
    }
  })
  
  // 주제 변경 타이머 시작
  startTopicChangeTimer()
  
  // WebSocket 연결
  try {
    await webSocket.connect()
    console.log('🔗 WebSocket 연결 성공')
  } catch (error) {
    console.error('❌ WebSocket 연결 실패:', error)
  }
})

onUnmounted(() => {
  console.log('🔍 Matching.vue 언마운트됨')
  
  // 타이머 정리
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  
  // WebSocket 연결 해제
  webSocket.disconnect()
  
  // 상태 초기화
  matchingState.reset()
  modals.hideAllModals()
})

// 주제 변경 타이머
const startTopicChangeTimer = () => {
  // 현재 주제 세트의 종료 시간 확인
  if (topicSetStore.currentSet?.endAtMs) {
    const update = () => {
      const now = Date.now()
      const end = topicSetStore.currentSet!.endAtMs
      const diff = end - now
      const seconds = Math.max(0, Math.floor(diff / 1000))
      remainingTime.value = seconds
    }
    update()
    timer = setInterval(update, 1000)
  } else {
    // 기본값으로 10분 설정
    remainingTime.value = 600
    timer = setInterval(() => {
      remainingTime.value--
      if (remainingTime.value <= 0) {
        remainingTime.value = 600
      }
    }, 1000)
  }
}
</script>