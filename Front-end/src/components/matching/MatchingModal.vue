<template>
  <Dialog :open="isOpen" :modal="false">
         <DialogContent class="sm:max-w-2xl" style="z-index: 9999; pointer-events: auto !important;">
      <DialogHeader>
        <DialogTitle class="text-center">🏛️ 매칭 성사!</DialogTitle>
        <DialogDescription class="text-center">
          매칭이 성사되었습니다. 수락하시겠습니까?
        </DialogDescription>
      </DialogHeader>
      
      <!-- 주제 정보 -->
      <div class="mb-4">
        <Card class="p-3">
          <div class="space-y-2">
            <div class="font-medium text-foreground">{{ topicTitle }}</div>
            <div class="text-sm space-y-2">
              <div class="flex items-center gap-2">
                <span class="font-medium text-muted-foreground">선택:</span>
                <Button 
                  size="sm" 
                  variant="outline"
                  class="h-6 px-2 text-xs"
                  :class="getStanceButtonClass(stanceText)"
                >
                  {{ stanceText }}
                </Button>
              </div>
              <div class="flex items-center gap-2">
                <span class="font-medium text-muted-foreground">모드:</span>
                <div class="flex gap-1">
                  <Button 
                    size="sm" 
                    variant="outline"
                    class="h-6 px-2 text-xs"
                    :class="getModeColorClass(mode) + ' ' + getModeTextClass(mode)"
                  >
                    {{ mode }}
                  </Button>
                </div>
              </div>
              <div class="text-xs text-muted-foreground mt-2">
                <span v-if="mode === '1:1'">• 1:1 토론 - 각 진영 1명씩</span>
                <span v-else-if="mode === '2:2'">• 2:2 토론 - 각 진영 2명씩</span>
              </div>
            </div>
          </div>
        </Card>
      </div>
      
      <!-- 연결 상태 -->
      <div class="mb-4">
        <div class="flex justify-center items-center gap-8 mb-2">
          <!-- 선택1팀 -->
          <Card class="p-8 bg-debate-left border-debate-left min-w-[200px]">
            <div class="flex flex-col items-center gap-4">
              <div class="text-lg font-medium text-slate-800 mb-4">{{ option1Name }}</div>
              <div class="flex gap-4">
                <!-- 수락한 사용자 아이콘들 -->
                <div 
                  v-for="i in acceptedCounts.option1" 
                  :key="`accepted-option1-${i}`"
                  class="flex flex-col items-center gap-3"
                >
                  <img 
                    :src="getUserIcon('option1')" 
                    class="w-12 h-12 rounded-full"
                    :alt="getUserIconAlt('option1')"
                  />
                  <span class="text-base text-slate-700">수락</span>
                </div>
                
                <!-- 대기 중인 사용자 아이콘들 -->
                <div 
                  v-for="i in (iconCount.left - acceptedCounts.option1)" 
                  :key="`waiting-option1-${i}`"
                  class="flex flex-col items-center gap-3"
                >
                  <img 
                    :src="waitingIcon" 
                    class="w-12 h-12 rounded-full"
                    alt="대기 중"
                  />
                  <span class="text-base text-slate-700">대기 중</span>
                </div>
              </div>
            </div>
          </Card>

          <!-- VS -->
          <div class="flex flex-col items-center">
            <div class="text-2xl font-bold text-muted-foreground">VS</div>
          </div>

          <!-- 선택2팀 -->
          <Card class="p-8 bg-debate-right border-debate-right min-w-[200px]">
            <div class="flex flex-col items-center gap-4">
              <div class="text-lg font-medium text-white mb-4">{{ option2Name }}</div>
              <div class="flex gap-4">
                <!-- 수락한 사용자 아이콘들 -->
                <div 
                  v-for="i in acceptedCounts.option2" 
                  :key="`accepted-option2-${i}`"
                  class="flex flex-col items-center gap-3"
                >
                  <img 
                    :src="getUserIcon('option2')" 
                    class="w-12 h-12 rounded-full"
                    :alt="getUserIconAlt('option2')"
                  />
                  <span class="text-base text-white">수락</span>
                </div>
                
                <!-- 대기 중인 사용자 아이콘들 -->
                <div 
                  v-for="i in (iconCount.right - acceptedCounts.option2)" 
                  :key="`waiting-option2-${i}`"
                  class="flex flex-col items-center gap-3"
                >
                  <img 
                    :src="waitingIcon" 
                    class="w-12 h-12 rounded-full"
                    alt="대기 중"
                  />
                  <span class="text-base text-white">대기 중</span>
                </div>
              </div>
            </div>
          </Card>
        </div>
        
        <!-- 연결 상태 표시 -->
        <div class="text-center">
          <p class="text-sm text-muted-foreground">
            연결된 사용자: {{ acceptedCounts.option1 + acceptedCounts.option2 }}/{{ totalCount }}
          </p>
          <p class="text-xs text-muted-foreground mt-1">
            <span v-if="mode === '1:1'">목표: 2명 (각 진영 1명씩)</span>
            <span v-else-if="mode === '2:2'">목표: 4명 (각 진영 2명씩)</span>
          </p>
        </div>
      </div>
      
      <!-- 타이머 및 진행률 -->
      <div class="mb-6">
        <div class="flex justify-between items-center mb-2">
          <span class="text-sm font-medium">수락 시간</span>
          <span class="text-sm text-muted-foreground">{{ timeLeft }}초 남음</span>
        </div>
        <div class="relative w-full bg-gray-200 rounded-full h-2">
          <div 
            class="bg-blue-600 h-2 rounded-full transition-all duration-1000 relative"
            :style="{ width: `${((30 - timeLeft) / 30) * 100}%` }"
          >
            <!-- 움직이는 run 아이콘 -->
            <img 
              :src="runIcon" 
              class="absolute -right-4 -top-3 w-8 h-8 animate-pulse"
              alt="진행 중"
            />
          </div>
        </div>
      </div>
      
                           <!-- 버튼들 -->
        <div class="flex gap-4">
          <Button 
            v-if="!isConnecting && !hasAccepted"
            @click="handleAccept"
            class="flex-1 w-full"
            size="lg"
          >
            수락
          </Button>
                         <div 
                  v-else-if="hasAccepted"
                  class="flex-1 flex items-center justify-center bg-debate-random text-white rounded-lg px-4 py-3 text-lg font-medium"
                >
                  수락 완료
                </div>
         <div 
           v-else-if="isConnecting"
           class="flex-1 flex items-center justify-center bg-debate-random text-white rounded-lg px-4 py-3 text-lg font-medium"
         >
            수락 완료
          </div>
          <Button 
            v-if="!isConnecting && !hasAccepted"
            @click="handleReject"
            variant="destructive"
            class="flex-1"
            size="lg"
          >
            거절
          </Button>
       </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { UserIcon, X, Loader2 } from 'lucide-vue-next'
import type { User } from '@/types/modal'
import { Card } from '@/components/ui/card'
import { useTopicSetStore } from '@/store/topicSet'
import { useAuthStore } from '@/store/auth'
import { useMatchingModals } from '@/composables/useMatchingModals'
import debateLeftIcon from '@/assets/images/profile/debate_left.png'
import debateRightIcon from '@/assets/images/profile/debate_right.png'
import debateRandomIcon from '@/assets/images/profile/debate_random.png'
import waitingIcon from '@/assets/images/profile/waiting.png'
import runIcon from '@/assets/images/profile/run.png'

interface Props {
  isOpen: boolean
  topicTitle: string
  stanceText: string
  mode: string
  topicId: number
  totalCount: number
  timeLeft: number
  isConnecting: boolean
  lastAcceptedStance?: string  // 마지막으로 수락한 진영 정보 추가
}

interface Emits {
  (e: 'accept'): void
  (e: 'reject'): void
  (e: 'update:isOpen', value: boolean): void
}

const props = withDefaults(defineProps<Props>(), {
  topicTitle: '매칭된 주제',
  stanceText: 'option2',
  mode: '1:1',
  topicId: 1,
  totalCount: 0,
  timeLeft: 30,
  isConnecting: false,
  lastAcceptedStance: undefined
})
const emit = defineEmits<Emits>()

// isOpen을 반응형으로 만들기
const isOpen = computed({
  get: () => props.isOpen,
  set: (value) => emit('update:isOpen', value)
})

// 주제 정보 가져오기
const topicSetStore = useTopicSetStore()
const authStore = useAuthStore()
const modals = useMatchingModals()

// 현재 사용자 아이디
const currentUserId = computed(() => authStore.user?.id || '')

// 현재 사용자가 수락했는지 확인 (로컬 상태 사용)
const localHasAccepted = ref(false)

const hasAccepted = computed(() => {
  return localHasAccepted.value
})

// 모달이 열릴 때 로컬 상태 초기화
watch(() => props.isOpen, (isOpen) => {
  if (isOpen) {
    localHasAccepted.value = false
    // 모달이 열릴 때 acceptedCounts 초기화
    // acceptedCounts.value = { option1: 0, option2: 0 } // 이 부분은 computed로 대체되므로 제거
    console.log('🔍 MatchingModal - 모달 열림, 카운트 초기화:', acceptedCounts.value)
  }
})

// props.lastAcceptedStance 변화 감지하여 카운트 업데이트
// watch(() => props.lastAcceptedStance, (newStance, oldStance) => {
//   console.log('🔍 MatchingModal - lastAcceptedStance 변화 감지:', {
//     이전값: oldStance,
//     새값: newStance
//   })
  
//   if (newStance && (newStance === 'option1' || newStance === 'option2')) {
//     console.log('🔍 MatchingModal - 마지막 수락한 진영 감지:', newStance)
//     const beforeCount = acceptedCounts.value[newStance as 'option1' | 'option2']
//     acceptedCounts.value[newStance as 'option1' | 'option2']++
//     const afterCount = acceptedCounts.value[newStance as 'option1' | 'option2']
//     console.log('🔍 수락 카운트 업데이트:', {
//       진영: newStance,
//       이전카운트: beforeCount,
//       새카운트: afterCount,
//       전체카운트: acceptedCounts.value
//     })
//   }
// })

const currentTopic = computed(() => {
  return topicSetStore.currentSet?.topics.find(topic => topic.id === props.topicId)
})

// 선택지 이름 가져오기
const option1Name = computed(() => currentTopic.value?.option1 || '선택1')
const option2Name = computed(() => currentTopic.value?.option2 || '선택2')

// 모드에 따른 아이콘 수 계산
const getIconCountByMode = (mode: string) => {
  switch (mode) {
    case '1:1':
      return { left: 1, right: 1 }
    case '2:2':
      return { left: 2, right: 2 }
    default:
      return { left: 1, right: 1 }
  }
}

// 현재 모드의 아이콘 수
const iconCount = computed(() => getIconCountByMode(props.mode))

// 각 진영별 수락한 사람 수 (stanceAcceptance 카운트 사용)
const acceptedCounts = computed(() => {
  const counts = {
    option1: modals.stanceAcceptance.value.option1,
    option2: modals.stanceAcceptance.value.option2
  }
  
  console.log('🔍 acceptedCounts 계산:', {
    stanceAcceptance: modals.stanceAcceptance.value,
    counts,
    iconCount: iconCount.value
  })
  
  return counts
})


// 모드에 따른 색상 클래스
const getModeColorClass = (mode: string) => {
  if (mode === '1:1') {
    return 'bg-mode-1v1 border-mode-1v1'
  } else if (mode === '2:2') {
    return 'bg-mode-2v2 border-mode-2v2'
  }
  return 'bg-gray-100 border-gray-300'
}

const getModeTextClass = (mode: string) => {
  if (mode === '1:1' || mode === '2:2') {
    return 'text-mode-1v1'
  }
  return 'text-gray-700'
}

const handleAccept = () => {
  console.log('🔍 MatchingModal - 수락 버튼 클릭됨')
  
  // 로컬 상태를 true로 설정
  localHasAccepted.value = true
  console.log('✅ localHasAccepted 상태 변경됨:', localHasAccepted.value)
  console.log('✅ hasAccepted computed 값:', hasAccepted.value)
  
  emit('accept')
}

const handleReject = () => {
  console.log('🔍 MatchingModal - 거절 버튼 클릭됨')
  emit('reject')
}

const getUserStatusClass = (user: User) => {
  if (user.accept === true) {
    return 'bg-green-100 border-green-300'
  } else if (user.accept === false) {
    return 'bg-red-100 border-red-300'
  } else {
    return 'bg-gray-100 border-gray-300'
  }
}

// 사용자 아이콘 가져오기 함수
const getUserIcon = (stance: string) => {
  switch (stance) {
    case 'option1':
      return debateLeftIcon
    case 'option2':
      return debateRightIcon
    case 'random':
      return debateRandomIcon
    default:
      return debateRandomIcon
  }
}

// 사용자 아이콘 alt 텍스트 가져오기 함수
const getUserIconAlt = (stance: string) => {
  switch (stance) {
    case 'option1':
      return '북극곰 아이콘'
    case 'option2':
      return '펭귄 아이콘'
    case 'random':
      return '물범 아이콘'
    default:
      return '사용자 아이콘'
  }
}

const getStanceButtonClass = (stance: string) => {
  if (stance === '찬성') {
    return 'bg-green-100 text-green-800 hover:bg-green-200'
  } else if (stance === '반대') {
    return 'bg-red-100 text-red-800 hover:bg-red-200'
  } else {
    return 'bg-gray-100 text-gray-800 hover:bg-gray-200'
  }
}

const getModeButtonClass = (mode: string) => {
  if (mode === '즉시') {
    return 'bg-blue-100 text-blue-800 hover:bg-blue-200'
  } else if (mode === '예약') {
    return 'bg-purple-100 text-purple-800 hover:bg-purple-200'
  } else {
    return 'bg-gray-100 text-gray-800 hover:bg-gray-200'
  }
}
</script> 