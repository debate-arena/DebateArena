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
    <div class="container mx-auto px-6 py-8">
      <div class="flex gap-8">
        <!-- 좌측: 매칭 선택 영역 -->
        <div class="flex-1 space-y-6">
          <!-- 주제 변경까지 남은 시간 -->
          <Card class="p-4 h-24 flex items-center justify-center">
            <div class="text-center">
              <p class="text-sm text-muted-foreground">주제 변경까지 남은 시간</p>
              <p class="text-2xl font-mono text-foreground">{{ formatTime(topicSetStore.remainingTimeSeconds) }}</p>
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
          <Card class="p-4 h-24 flex items-center justify-center">
            <div class="relative">
              <Button 
                @click="handleStartMatching"
                :disabled="!matchingStore.canStartMatching || matchingStore.isMatching"
                class="w-full"
                size="lg"
              >
                <span v-if="!matchingStore.isMatching">매칭 시작</span>
                <span v-else-if="matchingStore.status === 'matched' || matchingStore.status === 'connecting'">
                  매칭 성사
                </span>
                <span v-else class="flex items-center gap-2">
                  <div class="animate-spin rounded-full h-4 w-4 border-2 border-current border-t-transparent"></div>
                  매칭 대기 중...
                </span>
              </Button>
              
              <!-- 취소 버튼 (매칭 중일 때만 표시, 매칭 성사 시에는 숨김) -->
              <Button
                v-if="matchingStore.isMatching && matchingStore.status !== 'matched' && matchingStore.status !== 'connecting'"
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

          <!-- 게임 규칙 -->
          <Card class="p-4">
            <div class="space-y-3">
              <h3 class="text-sm font-medium text-foreground">게임 규칙</h3>
              <div class="text-xs space-y-2 text-muted-foreground">
                <div class="space-y-1">
                  <p class="font-medium">🎯 게임 진행</p>
                  <p>1. 준비 (30초)</p>
                  <p class="text-xs ml-2">– 주제를 빠르게 훑고, 내 주장의 핵심 아이디어를 쓱쓱 정리해요.</p>
                  <p>2. 입장 발표 (각 1분)</p>
                  <p class="text-xs ml-2">– 순서대로 돌아가며 1분 동안 내 입장을 솔직·담백하게 이야기해요.</p>
                  <p>3. 공격 대상 선정 (30초)</p>
                  <p class="text-xs ml-2">– "어떤 부분을 콕 집어 반박할까?" 30초 동안 고민해 보고 타깃을 골라요.</p>
                  <p>4. 공격 (30초)</p>
                  <p class="text-xs ml-2">– 선택한 주장에 대해 반박 포인트를 후려치듯 날려 봐요.</p>
                  <p>5. 방어 (30초)</p>
                  <p class="text-xs ml-2">– 공격받은 부분을 침착하게 수비하고, 내 논리를 다시 한번 단단히 다져요.</p>
                  <p>6. 선택 재투표 (30초)</p>
                  <p class="text-xs ml-2">– 2:2로 시작했다면, 토론 후 30초 동안 다시 어느 쪽을 지지할지 선택!</p>
                  <p>7. AI 판정 (무승부 시)</p>
                  <p class="text-xs ml-2">– 표 결과가 동률일 때만, AI가 논리 흐름·근거 제시 등을 보고 최종 승자를 골라 줘요.</p>
                </div>
                
                <div class="space-y-1">
                  <p class="font-medium">⚠️ 주의사항</p>
                  <p>1. 마이크를 허용해야 플레이가 가능해요</p>
                  <p>2. <img src="/src/assets/images/profile/debate_random.png" class="inline w-4 h-4" alt="물범" /> 선택시 <img src="/src/assets/images/profile/debate_left.png" class="inline w-4 h-4" alt="북극곰" />, <img src="/src/assets/images/profile/debate_right.png" class="inline w-4 h-4" alt="펭귄" />가 랜덤으로 선택되요</p>
                  <p>3. 발언 순서는 <img src="/src/assets/images/profile/debate_left.png" class="inline w-4 h-4" alt="북극곰" /> → <img src="/src/assets/images/profile/debate_right.png" class="inline w-4 h-4" alt="펭귄" /> 순서로 반복</p>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>

      <!-- 하단: 게임 통계 패널 -->
      <div class="mt-8">
        <GameStatsPanel />
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
      :total-count="modals.totalCount.value"
      :time-left="matchingStore.acceptTimeLeft"
      :is-connecting="matchingStore.status === 'connecting'"
      :last-accepted-stance="lastAcceptedStance"
      @accept="handleModalAccept"
      @reject="handleModalReject"
      @update:is-open="(value) => { if (!value) modals.hideMatchCompleteModal() }"
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

    <!-- 로그인 필요 모달 -->
    <Dialog v-model:open="modals.modalState.value.isLoginRequiredModalOpen" @update:open="handleLoginRequiredModalClose">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle class="text-center">
            🔐 로그인이 필요한 서비스입니다
          </DialogTitle>
          <DialogDescription class="text-center">
            매칭을 시작하려면 로그인이 필요합니다.
          </DialogDescription>
        </DialogHeader>
        
        <div class="space-y-4">
          <div class="text-center space-y-2">
            <p class="text-sm text-muted-foreground">
              Google 계정으로 로그인하여 매칭을 시작하세요.
            </p>
          </div>
          
          <div class="space-y-2">
            <Button size="lg" class="w-full" @click="handleLoginRequiredModalClose">확인</Button>
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
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { useMatchingStore } from '@/store/matching'
import { useTopicSetStore } from '@/store/topicSet'
import { useMatchingTimer } from '@/composables/useMatchingTimer'
import { useMatchingActions } from '@/composables/useMatchingActions'

import { useMatchingModals } from '@/composables/useMatchingModals'
import { useWebSocket } from '@/composables/useWebSocket'
import MatchingModal from '@/components/matching/MatchingModal.vue'
import { formatEstimatedTime, sortModes } from '@/utils/matching'
import PlayerCountSelection from '@/components/matching/PlayerCountSelection.vue'
import TopicCard from '@/components/matching/TopicCard.vue'
import type { Stance, PlayerMode, WebSocketMessage } from '@/types/matching'
import { UserIcon, AlertCircle } from 'lucide-vue-next'
import { useThemeStore } from '@/store/theme'
import { useAuthStore } from '@/store/auth'

// 아이콘 import
import debateLeftIcon from '@/assets/images/profile/debate_left.png'
import debateRightIcon from '@/assets/images/profile/debate_right.png'
import { CheckCircle } from 'lucide-vue-next'
import type { MatchModalData } from '@/composables/useMatchingModals'
import GameStatsPanel from '@/components/matching/GameStatsPanel.vue'

const router = useRouter()
const matchingStore = useMatchingStore()
const topicSetStore = useTopicSetStore()
const themeStore = useThemeStore()
const authStore = useAuthStore()

// 새로운 composable들 사용 (matchingState는 matchingStore로 통합)
const modals = useMatchingModals()
const actions = useMatchingActions()
const webSocket = useWebSocket()

// 타이머 관리
const { startMatchingTimer, stopMatchingTimer, startAcceptTimer, stopAcceptTimer } = useMatchingTimer()

// 타이머 관련 함수들
const remainingTime = ref(0)
let timer: ReturnType<typeof setInterval> | null = null
const lastAcceptedStance = ref<string | undefined>(undefined)
const formatTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
}

// WebSocket 메시지 처리
const handleWebSocketMessage = (data: WebSocketMessage) => {
  switch (data.type) {
    case 'MATCH_STATUS':
      // 매칭 현황판 처리
      if (data.status === 'error') {
        console.error('❌ 매칭 현황 에러:', data.data?.message || '알 수 없는 에러')
        break
      }
      
      // 성공인 경우에만 정상 처리
      if (data.status === 'success') {
        // 매칭 현황 처리 로직 (필요시 추가)
      }
      break
      
    case 'MATCH_INVITATION':
      // 매칭 초대장 처리
      if (data.status === 'error') {
        console.error('❌ 매칭 초대장 에러:', data.data?.message || '알 수 없는 에러')
        actions.handleError(data.data?.message || '매칭 초대장 처리 중 오류가 발생했습니다.')
        break
      }
      
      // 성공인 경우에만 정상 처리
      if (data.status === 'success') {
        // 백엔드에서 받은 실제 데이터 사용 (data.data가 실제 데이터)
        const invitationData = data.data || {}
        
        const matchId = invitationData.matchId
        const invitationTopicId = invitationData.topicId
        const invitationTeam = invitationData.team || 0
        const invitationType = invitationData.type || 0
        
        // 매칭 ID 저장
        matchingStore.setCurrentMatchId(matchId)
        
        // 팀 정보 저장 (서버에서 받은 팀 번호)
        matchingStore.setCurrentUserTeam(invitationTeam)
        
        // 서버 데이터를 UI 텍스트로 변환
        const invitationMode = invitationType === 0 ? '1:1' : '2:2'
        const invitationStance = invitationTeam === 0 ? 'option1' : 'option2'
        
        // 토픽 ID 타입 변환 (string -> number 또는 number -> number)
        const topicId = typeof invitationTopicId === 'string' ? parseInt(invitationTopicId) : invitationTopicId
        const invitationTopicTitle = topicSetStore.currentSet?.topics.find(t => t.id === topicId)?.title || '매칭된 주제'
        
        console.log('🔍 매칭 초대장 데이터:', {
          matchId,
          invitationTopicId,
          topicId,
          invitationTeam,
          invitationType,
          invitationMode,
          invitationStance,
          invitationTopicTitle,
          availableTopics: topicSetStore.currentSet?.topics.map(t => ({ id: t.id, title: t.title })) || []
        })
        
        // 매칭 초대장 모달 데이터 준비
        const modalData: MatchModalData = {
          topicTitle: invitationTopicTitle,
          stanceText: invitationStance,
          mode: invitationMode,
          topicId: topicId
        }
        
        // 매칭 초대장 모달 표시
        modals.showMatchCompleteModal(modalData)
        
        // 수락 타이머 시작
        matchingStore.startAcceptTimer()
        startAcceptTimer()
      }
      break
      
    case 'ACCEPTANCE_STATUS':
      // 다른 사람 응답 현황 처리 (내 수락도 포함)
      if (data.status === 'error') {
        console.error('❌ 응답 현황 에러:', data.data?.message || '알 수 없는 에러')
        break
      }
      
      // 성공인 경우에만 정상 처리
      if (data.status === 'success') {
        const accept = data.data?.accept
        const team = data.data?.team || 0      // 서버에서 받은 팀 정보
        const stance = team === 0 ? 'option1' : 'option2'  // 팀 정보로 진영 계산
        
        // 수락/거절 모두 카운트 증가 및 stance 업데이트
        matchingStore.updateRoomInfo({ connectedUsers: matchingStore.roomInfo.connectedUsers + 1 })
        
        if (accept) {
          // 수락 처리
          // 마지막 수락한 진영 정보 업데이트 (서버에서 받은 팀 정보 사용)
          lastAcceptedStance.value = stance
          
          // 모달의 진영별 수락 현황 업데이트 (서버에서 받은 팀 정보 사용)
          modals.updateStanceAcceptance(stance, accept)
        } else {
          // 거절 처리
          // 모달의 진영별 거절 현황 업데이트 (서버에서 받은 팀 정보 사용)
          modals.updateStanceAcceptance(stance, accept)
        }
      }
      break
      
    case 'ERROR':
      // 에러 처리
      if (data.status === 'error') {
        console.error('❌ 매칭 에러 수신됨!')
        actions.handleError(data.data?.message || '알 수 없는 에러')
      }
      break
      
    case 'MATCH_ADDITIONAL':
      // 추가 경로 메시지 처리
      if (data.status === 'error') {
        console.error('❌ 추가 경로 에러:', data.data?.message || '알 수 없는 에러')
        break
      }
      
      // 성공인 경우에만 정상 처리
      if (data.status === 'success') {
        // 추가 경로 처리 로직 (필요시 추가)
      }
      break
      
    case 'MATCH_ALL_PERSONAL':
      // 모든 개인 메시지 처리
      if (data.status === 'error') {
        console.error('❌ 개인 메시지 에러:', data.data?.message || '알 수 없는 에러')
        break
      }
      
      // 성공인 경우에만 정상 처리
      if (data.status === 'success') {
        // 개인 메시지 처리 로직 (필요시 추가)
      }
      break
      
    case 'MATCH_RESULT':
      // 매칭 결과 처리 (새로 추가)
      console.log('🎯 MATCH_RESULT 케이스 진입!')
      console.log('📥 MATCH_RESULT 원본 데이터:', data)
      
      if (data.status === 'error') {
        console.error('❌ 매칭 결과 에러:', data.data?.message || '알 수 없는 에러')
        break
      }
      
      // 성공인 경우에만 정상 처리
      if (data.status === 'success') {
        console.log('✅ MATCH_RESULT 성공 상태 확인됨')
        const resultData = data.data
        console.log('📥 MATCH_RESULT resultData:', resultData)
        
        // 데이터 구조 확인 및 안전한 접근
        if (resultData && (resultData.status === 'success' || resultData.roomId)) {
          // 매칭 성공 - 방 생성됨
          const roomId = resultData.roomId || resultData.data?.roomId
          console.log('🎉 매칭 성공! 방 ID:', roomId)
          
          if (roomId) {
            // 매칭 성공 시 처리
            console.log('🚀 handleMatchSuccess 호출 준비 중...')
            handleMatchSuccess(roomId.toString())
          } else {
            console.error('❌ roomId가 없습니다:', resultData)
          }
        } else {
          console.log('📥 매칭 결과 데이터 구조:', resultData)
          console.log('⚠️ roomId 또는 success 상태가 없음')
        }
      } else {
        console.log('⚠️ MATCH_RESULT가 success 상태가 아님:', data.status)
      }
      break
      
    default:
      console.log('⚠️ 알 수 없는 메시지 타입:', data.type)
      break
  }
}

// 매칭 시작 처리
const handleStartMatching = async () => {
  // 로그인 상태 확인 (가장 먼저)
  if (!authStore.isLoggedIn) {
    modals.showLoginRequiredModal()
    return
  }
  
  // 정각 5분 전(300초)인지 체크 (로그인 확인 후)
  if (remainingTime.value <= 300) {
    modals.showHourWarningModal()
  } else {
    // 바로 매칭 시작
    try {
      // WebSocket 연결 및 메시지 핸들러 설정
      await webSocket.connect()
      
      // 연결 성공 후 메시지 핸들러 설정
      webSocket.handleMessage(handleWebSocketMessage)
      
      // 매칭 요청 전송
      const request = matchingStore.toMatchRequest
      webSocket.sendMatchRequest(request)
      
      // 상태 업데이트
      matchingStore.startMatching()
      
      // 타이머 시작
      startMatchingTimer(() => {
        modals.showTimeoutModal()
      })
    } catch (error) {
      console.error('❌ 매칭 시작 실패:', error)
    }
  }
}

// 매칭 취소 처리
const handleCancelMatching = () => {
  matchingStore.cancelMatching()
  stopMatchingTimer()
  modals.hideAllModals()
}

// 모달 핸들러들
const handleModalAccept = () => {
  try {
    // 서버에 수락 메시지 전송
    const matchId = matchingStore.currentMatchId
    if (matchId) {
      // 서버에서 받은 실제 팀 정보 사용
      const teamNumber = matchingStore.getCurrentUserTeam()
      console.log('📤 매칭 수락 전송:', { matchId, accept: true, team: teamNumber })
      webSocket.sendMatchAcceptance(matchId, true, teamNumber)
    }
    
    // 타이머 정지
    matchingStore.stopAcceptTimer()
    stopAcceptTimer()
    
    // 프로그레스 바를 100%로 설정
    matchingStore.acceptTimeLeft = 0
  } catch (error) {
    console.error('❌ 매칭 수락 처리 실패:', error)
  }
}

const handleModalReject = () => {
  try {
    // 서버에 거절 메시지 전송
    const matchId = matchingStore.currentMatchId
    if (matchId) {
      // 서버에서 받은 실제 팀 정보 사용
      const teamNumber = matchingStore.getCurrentUserTeam()
      console.log('📤 매칭 거절 전송:', { matchId, accept: false, team: teamNumber })
      webSocket.sendMatchAcceptance(matchId, false, teamNumber)
    }
    
    // 타이머 정지
    matchingStore.stopAcceptTimer()
    stopAcceptTimer()
    
    // 프로그레스 바를 100%로 설정
    matchingStore.acceptTimeLeft = 0
  } catch (error) {
    console.error('❌ 매칭 거절 처리 실패:', error)
  }
}

const handleTimeoutModalClose = () => {
  modals.hideTimeoutModal()
  stopMatchingTimer()
  webSocket.disconnect()
  matchingStore.reset()
}

const handleTopicChangeModalClose = () => {
  console.log('handleTopicChangeModalClose 호출됨')
  modals.hideTopicChangeModal()
}

const handleHourWarningModalClose = () => {
  modals.hideHourWarningModal()
}

const handleLoginRequiredModalClose = () => {
  modals.hideLoginRequiredModal()
}

// 주제 변경 경고 확인
const confirmHourWarning = async () => {
  modals.hideHourWarningModal()
  
  // 로그인 상태 확인 (가장 먼저)
  if (!authStore.isLoggedIn) {
    modals.showLoginRequiredModal()
    return
  }
  
  // 바로 매칭 시작 (시간 체크 없이)
  try {
    // WebSocket 연결 및 메시지 핸들러 설정
    await webSocket.connect()
    
    // 연결 성공 후 메시지 핸들러 설정
    webSocket.handleMessage(handleWebSocketMessage)
    
    // 매칭 요청 전송
    const request = matchingStore.toMatchRequest
    webSocket.sendMatchRequest(request)
    
    // 상태 업데이트
    matchingStore.startMatching()
    
    // 타이머 시작
    startMatchingTimer(() => {
      modals.showTimeoutModal()
    })
  } catch (error) {
    console.error('❌ 매칭 시작 실패:', error)
  }
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

// 매칭 성공 시 처리
const handleMatchSuccess = (roomId: string) => {
  console.log('🎯 handleMatchSuccess 호출됨, roomId:', roomId)
  
  // 매칭 관련 상태 초기화
  matchingStore.cancelMatching()
  stopMatchingTimer()
  webSocket.disconnect()
  matchingStore.reset()
  modals.hideAllModals()
  
  // 토론방으로 라우팅
  console.log('🚀 토론방으로 라우팅:', `/debate-room/${roomId}`)
  router.push(`/debate-room/${roomId}`)
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
  
  // 글로벌 상태를 기본값으로 설정
  matchingStore.globalModes = new Set(['1:1', '2:2'])
  matchingStore.globalStances = new Set(['random'])
  
  // 주제별 초기 선택 생성
  matchingStore.initializeTopicSelections(activeTopics.map(topic => topic.id))
  
  // DOM 업데이트를 강제로 트리거
  await nextTick()
  
  // 타이머 만료 감시
  watch(() => matchingStore.error, (error) => {
    if (error === '수락 시간이 만료되었습니다.') {
      modals.hideMatchCompleteModal()
      matchingStore.clearError()
    }
  })
  
  // 모달 상태 감시
  watch(() => modals.modalState.value.isMatchCompleteModalOpen, (isOpen) => {
    // 모달이 열릴 때 lastAcceptedStance 초기화
    if (isOpen) {
      lastAcceptedStance.value = undefined
    }
  })
  
  // 주제 변경 타이머 시작
  startTopicChangeTimer()
})

onUnmounted(() => {
  // 타이머 정리
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  
  // WebSocket 연결 해제
  webSocket.disconnect()
  
  // 상태 초기화
  matchingStore.reset()
  modals.hideAllModals()
})

// 주제 변경 타이머
const startTopicChangeTimer = () => {
  // 현재 주제 세트의 종료 시간 확인
  if (topicSetStore.currentSet?.remainingTimeSeconds) {
    const update = () => {
      const remainingSeconds = topicSetStore.remainingTimeSeconds
      remainingTime.value = Math.max(0, remainingSeconds)
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