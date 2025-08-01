<template>
  <div class="min-h-screen bg-background">


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
                  v-for="selection in matchingStore.selectedTopicSelections" 
                  :key="selection.topicId"
                  class="text-sm"
                >
                  <div class="font-medium">{{ getTopicTitle(selection.topicId) }}</div>
                  <div class="text-muted-foreground">
                    진영: {{ getStanceText(selection.topicId, selection.stance) }} | 
                    모드: {{ selection.modes.join(', ') }}
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

    <!-- 모달들 -->
    <!-- 매칭 시작 확인 모달 -->
    <Dialog v-model:open="isStartModalOpen" @update:open="handleStartModalClose">
      <DialogContent class="sm:max-w-md no-backdrop">
        <DialogHeader>
          <DialogTitle class="text-center">매칭 시작</DialogTitle>
          <DialogDescription class="text-center">
            선택한 조건으로 매칭을 시작하시겠습니까?
          </DialogDescription>
        </DialogHeader>
        <div class="flex gap-2">
          <Button @click="confirmStartMatching" class="flex-1">
            시작
          </Button>
          <Button @click="isStartModalOpen = false" variant="outline" class="flex-1">
            취소
          </Button>
        </div>
      </DialogContent>
    </Dialog>
    
    <!-- 매칭 성사 모달 -->
    <Dialog v-model:open="isMatchCompleteModalOpen" @update:open="handleMatchCompleteModalClose">
      <DialogContent class="sm:max-w-md no-backdrop">
        <DialogHeader>
          <DialogTitle class="text-center">🎉 매칭 성사!</DialogTitle>
          <DialogDescription class="text-center">
            <div class="space-y-2">
              <p><strong>주제:</strong> {{ matchInfo.topicTitle }}</p>
              <p><strong>진영:</strong> {{ matchInfo.myStance }}</p>
              <p><strong>모드:</strong> {{ matchInfo.mode }}</p>
            </div>
          </DialogDescription>
        </DialogHeader>
        <div class="flex gap-2">
          <Button @click="acceptMatch" class="flex-1">
            수락
          </Button>
          <Button @click="rejectMatch" variant="outline" class="flex-1">
            거절
          </Button>
        </div>
      </DialogContent>
    </Dialog>
    
    <!-- 연결 중 모달 -->
    <Dialog v-model:open="isConnectingModalOpen" @update:open="handleConnectingModalClose">
      <DialogContent class="sm:max-w-md no-backdrop">
        <DialogHeader>
          <DialogTitle class="text-center">🔗 연결 중...</DialogTitle>
          <DialogDescription class="text-center">
            토론방에 연결하고 있습니다. 잠시만 기다려주세요.
          </DialogDescription>
        </DialogHeader>
        
                 <!-- 연결 상태 표시 -->
         <div class="space-y-4">
           <!-- 진영별 레이아웃 -->
           <div class="flex justify-center items-center gap-8">
             <!-- 선택1 진영 -->
             <div class="flex flex-col items-center gap-2">
               <div class="text-sm font-medium text-foreground">선택1</div>
               <div class="flex flex-col gap-2">
                 <div 
                   v-for="(user, index) in getStance1Users()" 
                   :key="user.userId"
                   class="flex flex-col items-center gap-1"
                 >
                   <div 
                     class="w-12 h-12 rounded-full flex items-center justify-center transition-all duration-300 border-2"
                     :class="getUserStatusClass(user)"
                   >
                     <img v-if="user.accept === true" :src="vikingIcon" class="w-6 h-6" alt="viking" />
                     <UserIcon v-else-if="user.accept === false" class="w-6 h-6 text-red-500" />
                     <div v-else class="w-6 h-6 rounded-full border-2 border-current"></div>
                   </div>
                   <span class="text-xs text-muted-foreground">
                     {{ getUserStatusText(user) }}
                   </span>
                 </div>
               </div>
             </div>
             
             <!-- VS 표시 -->
             <div class="text-lg font-bold text-foreground">VS</div>
             
             <!-- 선택2 진영 -->
             <div class="flex flex-col items-center gap-2">
               <div class="text-sm font-medium text-foreground">선택2</div>
               <div class="flex flex-col gap-2">
                 <div 
                   v-for="(user, index) in getStance2Users()" 
                   :key="user.userId"
                   class="flex flex-col items-center gap-1"
                 >
                   <div 
                     class="w-12 h-12 rounded-full flex items-center justify-center transition-all duration-300 border-2"
                     :class="getUserStatusClass(user)"
                   >
                     <img v-if="user.accept === true" :src="gladiatorIcon" class="w-6 h-6" alt="gladiator" />
                     <UserIcon v-else-if="user.accept === false" class="w-6 h-6 text-red-500" />
                     <div v-else class="w-6 h-6 rounded-full border-2 border-current"></div>
                   </div>
                   <span class="text-xs text-muted-foreground">
                     {{ getUserStatusText(user) }}
                   </span>
                 </div>
               </div>
             </div>
           </div>
           
           <div class="text-center">
             <p class="text-sm text-muted-foreground">
               {{ getConnectedCount() }}/{{ getTotalCount() }} 명 연결됨
             </p>
           </div>
         </div>
      </DialogContent>
    </Dialog>
    
    <!-- 타임아웃 모달 -->
    <Dialog v-model:open="isTimeoutModalOpen" @update:open="handleTimeoutModalClose">
      <DialogContent class="sm:max-w-md no-backdrop">
        <DialogHeader>
          <DialogTitle class="text-center">⏰ 매칭 타임아웃</DialogTitle>
          <DialogDescription class="text-center">
            매칭 시간이 초과되었습니다. 다시 시도해주세요.
          </DialogDescription>
        </DialogHeader>
        <div class="flex gap-2">
          <Button @click="isTimeoutModalOpen = false" class="flex-1">
            확인
          </Button>
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
  </div>
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

// WebSocket composable 사용
const webSocket = useWebSocket()
import { formatEstimatedTime } from '@/utils/matching'
import PlayerCountSelection from '@/components/matching/PlayerCountSelection.vue'
import TopicCard from '@/components/matching/TopicCard.vue'
import type { Stance, PlayerMode } from '@/types/matching'
import { UserIcon } from 'lucide-vue-next'
import { useThemeStore } from '@/store/theme'

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

// WebSocket 메시지 처리
const handleWebSocketMessage = (data: any) => {
  console.log('📨 WebSocket 메시지 처리:', data)
  console.log('🔍 메시지 타입:', data.type)
  console.log('🔍 메시지 데이터:', data.data)
  
  switch (data.type) {
    case 'MATCH_STATUS':
      // 매칭 현황판 처리
      console.log('📊 매칭 현황판:', data.data)
      // TODO: UI에 큐 상태 표시
      break
      
    case 'MATCH_INVITATION':
      // 매칭 초대장 처리 (status: "INFO")
      console.log('💌 매칭 초대장 수신됨!')
      console.log('🔍 매칭 초대장 데이터:', data.data)
      
      // matchId 저장
      currentMatchId.value = data.data.data.matchId
      console.log('🔍 매칭 ID 저장:', currentMatchId.value)
      
      // 백엔드에서 받은 모드 정보 사용 (실제로는 백엔드에서 전송)
      const receivedMode = data.data.data.mode || '1:1' // 기본값 1:1
      console.log('🔍 받은 모드 정보:', receivedMode)
      
      // 주제는 첫 번째 주제로 고정
      const invitationFirstTopic = topicSetStore.currentSet?.topics[0]
      const invitationTopicTitle = invitationFirstTopic?.title || '매칭된 주제'
      
      // 테마에 따라 진영 결정
      const invitationThemeStore = useThemeStore()
      const invitationStance = invitationThemeStore.isDark ? '선택2' : '선택1'
      
      console.log('🔍 매칭 정보:', {
        topicTitle: invitationTopicTitle,
        stance: invitationStance,
        mode: receivedMode,
        isDark: invitationThemeStore.isDark
      })
      
      // 모드 정보 저장 (수락 시 사용)
      currentMatchMode.value = receivedMode
      
      // 모달 표시
      showMatchCompleteModal(invitationTopicTitle, invitationStance, receivedMode)
      
      console.log('✅ 매칭 초대장 처리 완료')
      break
      
    case 'MATCH_SUCCESS':
      // 매칭 성사 처리 (status: "SUCCESS")
      console.log('🎉 매칭 성사 수신됨!')
      console.log('🔍 매칭 성사 데이터:', data.data)
      
      // 주제는 첫 번째 주제로 고정
      const successFirstTopic = topicSetStore.currentSet?.topics[0]
      const successTopicTitle = successFirstTopic?.title || '매칭된 주제'
      
      // 모드는 1:1로 고정
      const successMode = '1:1'
      
      // 테마에 따라 진영 결정
      const successThemeStore = useThemeStore()
      const successStance = successThemeStore.isDark ? '선택2' : '선택1'
      
      console.log('🔍 고정된 매칭 정보:', {
        topicTitle: successTopicTitle,
        stance: successStance,
        mode: successMode,
        isDark: successThemeStore.isDark
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
      // 매칭 실패 처리 (status: "WARNING")
      console.log('❌ 매칭 실패 수신됨!')
      console.log('🔍 매칭 실패 데이터:', data.data)
      
      // 매칭 상태 초기화
      matchingStore.isMatching = false
      matchingStore.status = 'idle'
      matchingStore.elapsedTime = 0
      stopMatchingTimer()
      
      // 실패 메시지 표시
      matchingStore.setError(data.data.data.message || '매칭이 취소되었습니다.')
      console.log('✅ 매칭 실패 처리 완료')
      break
      
         case 'ACCEPTANCE_STATUS':
       // 다른 사람 응답 현황 처리
       console.log('👥 다른 사람 응답 현황 수신됨!')
       console.log('🔍 응답 현황 데이터:', data.data)
       
       const user = data.data.data.user
       const accept = data.data.data.accept
       const stance = data.data.data.stance // 진영 정보
       
       console.log(`👤 ${user}님이 ${accept ? '수락' : '거부'}했습니다. (진영: ${stance})`)
       
       // 상대방 진영 계산 (테마에 따라 반대로 표시)
       const opponentStance = getOpponentStance(stance)
       
       // 사용자 상태 업데이트
       userAcceptanceStatus.value.set(user, {
         userId: user,
         accept: accept,
         stance: opponentStance,
         timestamp: Date.now()
       })
       
       console.log('✅ 사용자 수락 상태 업데이트 완료:', userAcceptanceStatus.value)
       break
      
    case 'ERROR':
      // 에러 처리
      console.error('❌ 매칭 에러 수신됨!')
      console.error('🔍 에러 데이터:', data.data)
      matchingStore.setError(data.data.message)
      break
      
    default:
      console.log('⚠️ 알 수 없는 메시지 타입:', data.type)
      break
  }
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

// 매칭 액션 관리 (cancelMatching은 사용하지 않음)
const { startMatching } = useMatchingActions()

// 연결 상태 관리
const roomInfo = ref({
  roomId: '',
  totalUsers: 0,
  connectedUsers: 0
})

// 현재 매칭 ID 저장
const currentMatchId = ref('')

// 현재 매칭 모드 저장
const currentMatchMode = ref('1:1')

// 사용자 수락 상태 관리
interface UserAcceptanceStatus {
  userId: string;
  nickname?: string;
  accept: boolean | null; // null = 대기중, true = 수락, false = 거절
  stance?: string; // "선택1", "선택2"
  timestamp: number;
}

const userAcceptanceStatus = ref<Map<string, UserAcceptanceStatus>>(new Map())

// 아이콘 import
import vikingIcon from '@/assets/images/profile/viking.png'
import gladiatorIcon from '@/assets/images/profile/gladiator.png'

let connectionInterval: ReturnType<typeof setInterval> | null = null

// 연결 진행 시뮬레이션 (백엔드 연동 전까지 임시 사용)
const simulateConnection = () => {
  console.log('simulateConnection 시작:', JSON.stringify(roomInfo.value))
  
  // 저장된 모드 정보 사용
  const mode = currentMatchMode.value
  const is1v1 = mode === '1:1'
  
  console.log(`시뮬레이션 시작: ${mode} 모드`)
  
  // 시뮬레이션: 1초마다 한 명씩 수락
  let currentUserIndex = 0
  const totalUsers = userAcceptanceStatus.value.size
  
  connectionInterval = setInterval(() => {
    if (currentUserIndex < totalUsers) {
      const users = Array.from(userAcceptanceStatus.value.values())
      const user = users[currentUserIndex]
      if (user) {
        user.accept = true
        userAcceptanceStatus.value.set(user.userId, user)
        console.log(`시뮬레이션: ${user.userId} 수락`)
      }
      currentUserIndex++
    } else {
      if (connectionInterval) {
        clearInterval(connectionInterval)
        connectionInterval = null
      }
      // 자동으로 넘어가지 않고 모달만 닫기
      hideConnectingModal()
      console.log('✅ 모든 사용자 연결 완료 - 모달 닫힘')
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

// 진영별 사용자 분류
const getStance1Users = () => {
  return Array.from(userAcceptanceStatus.value.values())
    .filter(user => user.stance === '선택1')
    .sort((a, b) => a.timestamp - b.timestamp)
}

const getStance2Users = () => {
  return Array.from(userAcceptanceStatus.value.values())
    .filter(user => user.stance === '선택2')
    .sort((a, b) => a.timestamp - b.timestamp)
}

// 사용자 상태에 따른 CSS 클래스
const getUserStatusClass = (user: UserAcceptanceStatus) => {
  if (user.accept === true) {
    // 수락한 상태 - 진영에 따른 배경색
    if (user.stance === '선택1') {
      return 'bg-debate-left text-white border-debate-left'
    } else if (user.stance === '선택2') {
      return 'bg-debate-right text-white border-debate-right'
    } else {
      return 'bg-green-500 text-white border-green-500'
    }
  } else if (user.accept === false) {
    return 'bg-red-500 text-white border-red-500'
  } else {
    // 대기중 상태 - 테마에 따른 빈 원
    const themeStore = useThemeStore()
    return themeStore.isDark 
      ? 'bg-gray-800 border-white text-white' 
      : 'bg-white border-gray-900 text-gray-900'
  }
}

// 사용자 상태 텍스트
const getUserStatusText = (user: UserAcceptanceStatus) => {
  if (user.accept === true) {
    return '수락함'
  } else if (user.accept === false) {
    return '거절함'
  } else {
    return '대기중'
  }
}

// 연결된 사용자 수
const getConnectedCount = () => {
  return Array.from(userAcceptanceStatus.value.values())
    .filter(user => user.accept === true).length
}

// 전체 사용자 수
const getTotalCount = () => {
  return userAcceptanceStatus.value.size
}

// 상대방 진영 계산 (테마에 따라 반대로 표시)
const getOpponentStance = (receivedStance: string) => {
  const themeStore = useThemeStore()
  
  if (receivedStance === '선택1') {
    return themeStore.isDark ? '선택2' : '선택1'
  } else if (receivedStance === '선택2') {
    return themeStore.isDark ? '선택1' : '선택2'
  }
  
  return receivedStance // '상관없음' 등은 그대로
}

// 모드별 연결 상태 초기화 (백엔드 연동 시 사용)
const initializeConnectionStatus = (mode: string) => {
  const is1v1 = mode === '1:1'
  
  // 기존 상태 초기화
  userAcceptanceStatus.value.clear()
  
  // 모드에 따른 사용자 생성
  if (is1v1) {
    // 1:1 모드 - 2명
    const users = [
      { userId: 'user1', stance: '선택1', accept: null },
      { userId: 'user2', stance: '선택2', accept: null }
    ]
    
    users.forEach(user => {
      userAcceptanceStatus.value.set(user.userId, {
        userId: user.userId,
        accept: user.accept,
        stance: user.stance,
        timestamp: Date.now()
      })
    })
  } else {
    // 2:2 모드 - 4명
    const users = [
      { userId: 'user1', stance: '선택1', accept: null },
      { userId: 'user2', stance: '선택1', accept: null },
      { userId: 'user3', stance: '선택2', accept: null },
      { userId: 'user4', stance: '선택2', accept: null }
    ]
    
    users.forEach(user => {
      userAcceptanceStatus.value.set(user.userId, {
        userId: user.userId,
        accept: user.accept,
        stance: user.stance,
        timestamp: Date.now()
      })
    })
  }
  
  console.log(`연결 상태 초기화: ${mode} 모드, ${userAcceptanceStatus.value.size}명`)
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
const handleStartMatching = async () => {
  // 정각 5분 전(300초)인지 체크
  if (remainingTime.value <= 300) {
    showHourWarningModal()
  } else {
    // WebSocket 연결 후 매칭 시작
    try {
      await webSocket.connect()
      
      // 매칭 요청 전송
      const request = matchingStore.toMatchRequest
      webSocket.sendMatchRequest(request)
      
      // 매칭 상태 업데이트
      matchingStore.isMatching = true
      matchingStore.status = 'waiting'
      matchingStore.elapsedTime = 0
      
      // 타이머 시작
      startMatchingTimer(handleTimeout)
      
      // 메시지 수신 처리
      webSocket.handleMessage(handleWebSocketMessage)
      
    } catch (error) {
      console.error('❌ 매칭 시작 실패:', error)
      matchingStore.setError('매칭 시작에 실패했습니다.')
    }
  }
}

// 매칭 취소 처리
const handleCancelMatching = () => {
  console.log('❌ 매칭 취소 요청')
  console.log('📊 취소 시점 상태:', {
    isMatching: matchingStore.isMatching,
    status: matchingStore.status,
    elapsedTime: matchingStore.elapsedTime
  })
  
  // 프론트 상태만 변경 (백엔드 메시지 전송 안함)
  matchingStore.isMatching = false
  matchingStore.status = 'idle'
  matchingStore.elapsedTime = 0
  stopMatchingTimer()
  hideStartModal()
  hideHourWarningModal()
  
  console.log('✅ 매칭 취소 완료 (프론트 상태만 변경)')
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
  console.log('🔍 acceptMatch 호출됨')
  console.log('🔍 수락 전 모달 상태:', isMatchCompleteModalOpen.value)
  
  // 서버에 매칭 수락 메시지 전송
  const matchId = currentMatchId.value || 'default-match-id'
  console.log('📤 서버에 매칭 수락 전송:', { matchId, accept: true })
  webSocket.sendMatchAcceptance(matchId, true)
  
  hideMatchCompleteModal()
  console.log('🔍 수락 후 모달 상태:', isMatchCompleteModalOpen.value)
  showConnectingModal()
  console.log('🔍 연결 모달 표시됨')
  
  // 저장된 모드 정보로 연결 상태 초기화
  const mode = currentMatchMode.value
  console.log('🔍 사용할 모드:', mode)
  initializeConnectionStatus(mode)
  
  // 내 상태를 바로 수락으로 업데이트
  const myUserId = 'user1' // 실제로는 현재 사용자 ID 사용
  const myUser = userAcceptanceStatus.value.get(myUserId)
  if (myUser) {
    myUser.accept = true
    userAcceptanceStatus.value.set(myUserId, myUser)
    console.log('✅ 내 상태를 수락으로 업데이트:', myUserId)
  }
  
  simulateConnection()
  console.log('🔍 연결 시뮬레이션 시작됨')
}

// 매칭 거부
const rejectMatch = () => {
  console.log('🔍 rejectMatch 호출됨')
  console.log('🔍 거부 전 모달 상태:', isMatchCompleteModalOpen.value)
  
  // 서버에 매칭 거부 메시지 전송
  const matchId = currentMatchId.value || 'default-match-id'
  console.log('📤 서버에 매칭 거부 전송:', { matchId, accept: false })
  webSocket.sendMatchAcceptance(matchId, false)
  
  hideMatchCompleteModal()
  console.log('🔍 거부 후 모달 상태:', isMatchCompleteModalOpen.value)
  
  // 매칭 상태 초기화
  matchingStore.isMatching = false
  matchingStore.status = 'idle'
  matchingStore.elapsedTime = 0
  stopMatchingTimer()
  
  console.log('✅ 매칭 거부 완료')
}

// 모든 매칭 취소
const cancelAllMatches = () => {
  console.log('❌ 모든 매칭 취소 요청')
  
  // 프론트 상태만 변경 (백엔드 메시지 전송 안함)
  matchingStore.isMatching = false
  matchingStore.status = 'idle'
  matchingStore.elapsedTime = 0
  stopMatchingTimer()
  
  hideMatchCompleteModal()
  hideConnectingModal()
  if (connectionInterval) {
    clearInterval(connectionInterval)
    connectionInterval = null
  }
  roomInfo.value = { roomId: '', totalUsers: 0, connectedUsers: 0 }
  
  console.log('✅ 모든 매칭 취소 완료 (프론트 상태만 변경)')
}

// 연결 모달 닫기
const handleConnectingModalClose = () => {
  hideConnectingModal()
  matchingStore.cancelMatching()
  stopMatchingTimer()
  webSocket.disconnect()
  if (connectionInterval) {
    clearInterval(connectionInterval)
    connectionInterval = null
  }
  roomInfo.value = { roomId: '', totalUsers: 0, connectedUsers: 0 }
  // 사용자 수락 상태 초기화
  userAcceptanceStatus.value.clear()
  // 모드 정보 초기화
  currentMatchMode.value = '1:1'
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
  webSocket.disconnect()
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
  if (connectionInterval) {
    clearInterval(connectionInterval)
    connectionInterval = null
  }
  roomInfo.value = { roomId: '', totalUsers: 0, connectedUsers: 0 }
  
  // 사용자 수락 상태 초기화
  userAcceptanceStatus.value.clear()
  
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
    topicSelections: matchingStore.topicSelections
  })
  
  // WebSocket 연결
  try {
    await webSocket.connect()
    console.log('🔗 WebSocket 연결 성공')
  } catch (error) {
    console.error('❌ WebSocket 연결 실패:', error)
  }
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
  // 프론트 상태만 변경 (백엔드 메시지 전송 안함)
  matchingStore.isMatching = false
  matchingStore.status = 'idle'
  matchingStore.elapsedTime = 0
  stopMatchingTimer()
  webSocket.disconnect()
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
  webSocket.disconnect()
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
  webSocket.disconnect()
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

// 매칭 정보 (동적 설정)
const matchInfo = computed(() => {
  const firstTopic = topicSetStore.currentSet?.topics[0]
  const topicTitle = firstTopic?.title || '매칭된 주제'
  const mode = '1:1'
  
  // 테마에 따라 진영 결정
  const themeStore = useThemeStore()
  const stance = themeStore.isDark ? '선택2' : '선택1'
  
  return {
    topicId: firstTopic?.id || 1,
    stance: 'option1' as Stance,
    mode: mode as PlayerMode,
    topicTitle: topicTitle,
    myStance: stance
  }
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