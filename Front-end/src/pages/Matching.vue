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
import type { Stance, PlayerMode } from '@/types/matching'
import { UserIcon, AlertCircle } from 'lucide-vue-next'
import { useThemeStore } from '@/store/theme'
import { useAuthStore } from '@/store/auth'

// 아이콘 import
import debateLeftIcon from '@/assets/images/profile/debate_left.png'
import debateRightIcon from '@/assets/images/profile/debate_right.png'
import { CheckCircle } from 'lucide-vue-next'

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
      console.log('🔍 매칭 초대장 전체 구조:', JSON.stringify(data, null, 2))
      
      // 백엔드에서 받은 실제 데이터 사용
      const invitationData = data.data.data
      const matchId = invitationData.matchId // 서버에서 받아옴
      const invitationMode = '1:1' // 고정값
      const invitationStance = 'option1' // 고정값
      const invitationTopicId = 1 // 고정값
      const invitationTopicTitle = topicSetStore.currentSet?.topics.find(t => t.id === invitationTopicId)?.title || '매칭된 주제' // store에서 조회
      const invitationUsers: any[] = [] // 백엔드에서 전송하지 않음
      
      console.log('🔍 실제 초대장 데이터:', {
        matchId,
        topicTitle: invitationTopicTitle,
        mode: invitationMode,
        stance: invitationStance,
        topicId: invitationTopicId,
        users: invitationUsers
      })
      
      // 유저 데이터 상세 분석
      console.log('🔍 유저 데이터 분석:', {
        usersExists: !!invitationUsers,
        usersType: typeof invitationUsers,
        usersIsArray: Array.isArray(invitationUsers),
        usersLength: invitationUsers?.length || 0,
        usersContent: invitationUsers
      })
      
      // 매칭 성사 처리 (사용자 데이터 포함)
      actions.handleMatchSuccess({
        matchId: matchId,
        topicTitle: invitationTopicTitle,
        stanceText: invitationStance,
        mode: invitationMode,
        topicId: invitationTopicId,
        users: invitationUsers
      })
      
      console.log('✅ 매칭 초대장 처리 완료')
      break
      
    case 'MATCH_SUCCESS':
      // 매칭 성사 처리
      console.log('🎉 매칭 성사 수신됨!')
      console.log('🔍 매칭 성사 데이터:', data.data)
      console.log('🔍 현재 모달 상태 (처리 전):', modals.modalState.value)
      
      // 타이머 정지 (모달 닫기는 부분 제거)
      matchingStore.stopAcceptTimer()
      stopAcceptTimer()
      
      // 백엔드에서 받은 실제 데이터 사용 (모의 데이터로 대체)
      const matchData = data.data?.data || data.data || {}
      const successMatchId = matchData.matchId || 'test-match-123'
      const successMode = '1:1' // 고정값
      const successStance = 'option1' // 고정값
      const successTopicId = 1 // 고정값
      const successTopicTitle = topicSetStore.currentSet?.topics.find(t => t.id === successTopicId)?.title || '매칭된 주제' // store에서 조회
      const successUsers: any[] = [] // 백엔드에서 전송하지 않음
      
      // 현재 사용자의 position 설정 (서버에서 받은 정보 기반)
      const currentUserPosition = matchData.currentUserPosition || 0 // 서버에서 받지 못하면 기본값 0
      matchingStore.setCurrentUserPosition(currentUserPosition)
      console.log('🔍 현재 사용자 position 설정:', currentUserPosition)
      
      console.log('🔍 실제 매칭 데이터:', {
        matchId: successMatchId,
        topicTitle: successTopicTitle,
        mode: successMode,
        stance: successStance,
        topicId: successTopicId,
        users: successUsers,
        currentUserPosition
      })
      
      // 유저 데이터 상세 분석
      console.log('🔍 MATCH_SUCCESS 유저 데이터 분석:', {
        usersExists: !!successUsers,
        usersType: typeof successUsers,
        usersIsArray: Array.isArray(successUsers),
        usersLength: successUsers?.length || 0,
        usersContent: successUsers
      })
      
      // 매칭 성사 처리 (사용자 데이터 포함)
      console.log('🔍 handleMatchSuccess 호출 전')
      console.log('🔍 actions 객체 확인:', !!actions)
      console.log('🔍 actions.handleMatchSuccess 확인:', !!actions.handleMatchSuccess)
      
      // 매칭 타이머 정지 (매칭 성사 시)
      stopMatchingTimer()
      console.log('🔍 매칭 타이머 정지됨')
      
      // 매칭 성사 상태로 설정
      matchingStore.isMatching = true
      matchingStore.status = 'matched'
      console.log('🔍 매칭 성사 상태로 설정됨')
      console.log('🔍 매칭 성사 후 상태 확인:', {
        isMatching: matchingStore.isMatching,
        status: matchingStore.status
      })
      
      // 페이지 타이머 정지 (remainingTime)
      if (timer) {
        clearInterval(timer)
        timer = null
        console.log('🔍 페이지 타이머 정지됨')
      }
      
      actions.handleMatchSuccess({
        matchId: successMatchId,
        topicTitle: successTopicTitle,
        stanceText: successStance,
        mode: successMode,
        topicId: successTopicId,
        users: successUsers
      })
      console.log('🔍 handleMatchSuccess 호출 후')
      console.log('🔍 현재 모달 상태 (처리 후):', modals.modalState.value)
      console.log('🔍 모달 데이터:', modals.matchModalData.value)
      
      // 토론방으로 이동 (모달에서 수락 후 이동하므로 여기서는 제거)
      // if (matchData.roomUrl) {
      //   console.log('🔍 토론방으로 이동:', matchData.roomUrl)
      //   router.push(matchData.roomUrl)
      // } else {
      //   console.log('🔍 기본 토론방으로 이동: /debate')
      //   router.push('/debate')
      // }
      break
      
    case 'MATCH_FAILURE':
      // 매칭 실패 처리
      console.log('❌ 매칭 실패 수신됨!')
      console.log('🔍 매칭 실패 데이터:', data.data)
      
      // 타이머 정지 및 모달 닫기
      matchingStore.stopAcceptTimer()
      stopAcceptTimer()
      modals.hideMatchCompleteModal()
      
      actions.handleError(data.data.data.message || '매칭이 취소되었습니다.')
      break
      
    case 'ACCEPTANCE_STATUS':
      // 다른 사람 응답 현황 처리 (내 수락도 포함)
      console.log('👥 응답 현황 수신됨!')
      console.log('🔍 응답 현황 데이터:', data.data)
      console.log('🔍 전체 이벤트 구조:', JSON.stringify(data, null, 2))
      
      const accept = data.data.data.accept
      const choice = data.data.data.choice      // 0: 찬성, 1: 반대, 2: 상관없음
      
      // 백엔드에서 받은 choice 값을 사용 (실제 선택한 진영)
      // 백엔드에서 받은 전체 수락 순서를 사용 (모든 사용자가 동일한 순서)
      const totalAcceptCount = data.data.data.totalAcceptCount || (matchingStore.roomInfo.connectedUsers + 1)
      const position = (totalAcceptCount - 1) % 2  // 0: 첫 번째 (option1), 1: 두 번째 (option2)
      const stance = position === 0 ? 'option1' : 'option2'
      
      console.log('🔍 파싱된 응답 현황 정보:', {
        accept,
        choice,
        totalAcceptCount,
        position,
        stance
      })
      
      // 수락/거절 모두 카운트 증가 및 stance 업데이트
      const beforeConnected = matchingStore.roomInfo.connectedUsers
      matchingStore.updateRoomInfo({ connectedUsers: matchingStore.roomInfo.connectedUsers + 1 })
      const afterConnected = matchingStore.roomInfo.connectedUsers
      console.log('🔍 연결된 사용자 수 업데이트:', { before: beforeConnected, after: afterConnected })
      
      if (accept) {
        // 수락 처리
        console.log('✅ 다른 사람이 수락함')
        
        // 마지막 수락한 진영 정보 업데이트 (계산된 stance 사용)
        lastAcceptedStance.value = stance
        console.log('🔍 마지막 수락한 진영 업데이트:', stance)
        console.log('🔍 lastAcceptedStance 값:', lastAcceptedStance.value)
        
        // 모달의 진영별 수락 현황 업데이트 (계산된 stance 사용)
        modals.updateStanceAcceptance(stance, accept)
        console.log('🔍 모달 진영별 수락 현황 업데이트 완료')
        
        // 디버깅: 전체 데이터 구조 확인
        console.log('🔍 ACCEPTANCE_STATUS 전체 데이터:', {
          accept,
          choice,
          totalAcceptCount,
          position,
          stance,
          connectedUsers: matchingStore.roomInfo.connectedUsers,
          lastAcceptedStance: lastAcceptedStance.value,
          stanceAcceptance: modals.stanceAcceptance.value
        })
      } else {
        // 거절 처리
        console.log('❌ 다른 사람이 거절함')
        
        // 모달의 진영별 거절 현황 업데이트 (계산된 stance 사용)
        modals.updateStanceAcceptance(stance, accept)
        console.log('🔍 모달 진영별 거절 현황 업데이트 완료')
        
        // 디버깅: 전체 데이터 구조 확인
        console.log('🔍 ACCEPTANCE_STATUS 거절 데이터:', {
          accept,
          choice,
          totalAcceptCount,
          position,
          stance,
          connectedUsers: matchingStore.roomInfo.connectedUsers,
          lastAcceptedStance: lastAcceptedStance.value,
          stanceAcceptance: modals.stanceAcceptance.value
        })
      }
      
      console.log(`✅ ${stance} 진영 ${accept ? '수락' : '거절'} - ${matchingStore.roomInfo.connectedUsers}/${matchingStore.roomInfo.totalUsers}`)
      break
      
    case 'ERROR':
      // 에러 처리
      console.error('❌ 매칭 에러 수신됨!')
      console.error('🔍 에러 데이터:', data.data)
      actions.handleError(data.data.message)
      break
      
    case 'MATCH_ADDITIONAL':
      // 추가 경로 메시지 처리
      console.log('📨 추가 경로 메시지 수신됨!')
      console.log('🔍 추가 경로 데이터:', data.data)
      break
      
    case 'MATCH_ALL_PERSONAL':
      // 모든 개인 메시지 처리
      console.log('📨 모든 개인 메시지 수신됨!')
      console.log('🔍 모든 개인 메시지 데이터:', data.data)
      break
      
    default:
      console.log('⚠️ 알 수 없는 메시지 타입:', data.type)
      break
  }
}

// 매칭 시작 처리
const handleStartMatching = async () => {
  // 로그인 상태 확인
  if (!authStore.isLoggedIn) {
    console.log('❌ 로그인되지 않은 상태에서 매칭 시작 시도')
    modals.showLoginRequiredModal()
    return
  }
  
  // 정각 5분 전(300초)인지 체크
  if (remainingTime.value <= 300) {
    modals.showHourWarningModal()
  } else {
    // 바로 매칭 시작
    console.log('🚀 매칭 시작!')
    
    try {
      // WebSocket 연결 및 메시지 핸들러 설정
      await webSocket.connect()
      console.log('🔗 WebSocket 연결 성공')
      
      // 연결 성공 후 메시지 핸들러 설정
      console.log('🔍 WebSocket 메시지 핸들러 설정 시도...')
      webSocket.handleMessage(handleWebSocketMessage)
      console.log('✅ WebSocket 메시지 핸들러 설정 완료')
      
      // 매칭 요청 전송
      const request = matchingStore.toMatchRequest
      webSocket.sendMatchRequest(request)
      console.log('📤 매칭 요청 전송됨:', request)
      
      // 상태 업데이트
      matchingStore.startMatching()
      
      // 타이머 시작
      startMatchingTimer(() => {
        console.log('⏰ 매칭 타임아웃')
        modals.showTimeoutModal()
      })
      
      console.log('✅ 매칭 시작 완료')
    } catch (error) {
      console.error('❌ 매칭 시작 실패:', error)
    }
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
  
  try {
    // 서버에 수락 메시지 전송
    const matchId = matchingStore.currentMatchId
    if (matchId) {
      // 실제 stance를 숫자로 변환해서 전송
      const actualStance = 'option1'  // 실제 선택한 진영
      const stanceNumber = actualStance === 'option1' ? 0 : 1  // 0: option1, 1: option2
      webSocket.sendMatchAcceptance(matchId, true, stanceNumber)
      console.log(`📤 수락 메시지 전송됨 (stance: ${actualStance}, number: ${stanceNumber})`)
    }
    
    // 타이머 정지
    matchingStore.stopAcceptTimer()
    stopAcceptTimer()
    console.log('✅ 타이머 정지됨')
    
    // 프로그레스 바를 100%로 설정
    matchingStore.acceptTimeLeft = 0
    console.log('✅ 프로그레스 바 100% 설정됨')
    
    console.log('✅ 매칭 수락 처리 완료 - 다른 사람 응답 대기 중...')
  } catch (error) {
    console.error('❌ 매칭 수락 처리 실패:', error)
  }
}

const handleModalReject = () => {
  console.log('❌ 매칭 거절 버튼 클릭됨')
  
  try {
    // 서버에 거절 메시지 전송
    const matchId = matchingStore.currentMatchId
    if (matchId) {
      // 실제 stance를 숫자로 변환해서 전송
      const actualStance = 'option1'  // 실제 선택한 진영
      const stanceNumber = actualStance === 'option1' ? 0 : 1  // 0: option1, 1: option2
      webSocket.sendMatchAcceptance(matchId, false, stanceNumber)
      console.log(`✅ 거절 메시지 전송됨 (stance: ${actualStance}, number: ${stanceNumber})`)
    } else {
      console.warn('⚠️ matchId가 없음')
    }
    
    // 타이머 정지
    matchingStore.stopAcceptTimer()
    stopAcceptTimer()
    console.log('✅ 타이머 정지됨')
    
    // 프로그레스 바를 100%로 설정
    matchingStore.acceptTimeLeft = 0
    console.log('✅ 프로그레스 바 100% 설정됨')
    
    console.log('❌ 매칭 거절 처리 완료')
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
  
  // 타이머 만료 감시
  watch(() => matchingStore.error, (error) => {
    if (error === '수락 시간이 만료되었습니다.') {
      console.log('⏰ 타이머 만료로 모달 닫기')
      modals.hideMatchCompleteModal()
      matchingStore.clearError()
    }
  })
  
  // 모달 상태 감시
  watch(() => modals.modalState.value.isMatchCompleteModalOpen, (isOpen) => {
    console.log('🔍 모달 상태 변경 감지:', isOpen)
    console.log('🔍 전체 모달 상태:', modals.modalState.value)
    console.log('🔍 모달 데이터:', modals.matchModalData.value)
    
    // 모달이 열릴 때 추가 디버깅
    if (isOpen) {
      console.log('🎉 모달이 열렸습니다!')
      console.log('🔍 모달 Props 확인:')
      console.log('  - isOpen:', modals.modalState.value.isMatchCompleteModalOpen)
      console.log('  - topicTitle:', modals.matchModalData.value.topicTitle)
      console.log('  - stanceText:', modals.matchModalData.value.stanceText)
      console.log('  - mode:', modals.matchModalData.value.mode)
      console.log('  - topicId:', modals.matchModalData.value.topicId)
      console.log('  - totalCount:', modals.totalCount.value)
      console.log('  - timeLeft:', matchingStore.acceptTimeLeft)
      console.log('  - isConnecting:', matchingStore.status === 'connecting')
      
      // 모달이 열릴 때 lastAcceptedStance 초기화
      lastAcceptedStance.value = undefined
      console.log('🔍 lastAcceptedStance 초기화됨')
    }
  })
  
  // 주제 변경 타이머 시작
  startTopicChangeTimer()
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
  matchingStore.reset()
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