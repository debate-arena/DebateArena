<template>
  <Dialog v-model:open="isOpen" :modal="false">
    <DialogContent class="sm:max-w-lg">
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
                  {{ stanceText === 'option1' ? option1Name : option2Name }}
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
        <div class="flex justify-center items-center gap-4 mb-2">
          <!-- 선택1팀 -->
          <Card class="p-4 bg-debate-left border-debate-left min-w-[140px]">
            <div class="flex flex-col items-center gap-2">
              <div class="text-sm font-medium text-slate-800 mb-2">{{ option1Name }}</div>
              <div class="flex gap-2">
                <!-- 수락한 사용자 아이콘들 -->
                <div 
                  v-for="i in stanceCounts.option1.accept" 
                  :key="`accepted-option1-${i}`"
                  class="flex flex-col items-center gap-1"
                >
                  <img 
                    :src="getUserIcon('option1')" 
                    class="w-24 h-24 rounded-full"
                    :alt="getUserIconAlt('option1')"
                  />
                  <span class="text-xs text-slate-700">수락</span>
                </div>
                
                <!-- 대기 중인 사용자 아이콘들 -->
                <div 
                  v-for="i in stanceCounts.option1.waiting" 
                  :key="`waiting-option1-${i}`"
                  class="flex flex-col items-center gap-1"
                >
                  <img 
                    :src="waitingIcon" 
                    class="w-24 h-24 rounded-full"
                    alt="대기 중"
                  />
                  <span class="text-xs text-slate-700">대기 중</span>
                </div>
                
                <!-- 거절한 사용자 아이콘들 -->
                <div 
                  v-for="i in stanceCounts.option1.reject" 
                  :key="`rejected-option1-${i}`"
                  class="flex flex-col items-center gap-1"
                >
                  <img 
                    :src="rejectIcon" 
                    class="w-24 h-24 rounded-full"
                    alt="거절"
                  />
                  <span class="text-xs text-slate-700">거절</span>
                </div>
              </div>
            </div>
          </Card>

          <!-- VS -->
          <div class="flex flex-col items-center">
            <div class="text-lg font-bold text-muted-foreground">VS</div>
          </div>

          <!-- 선택2팀 -->
          <Card class="p-4 bg-debate-right border-debate-right min-w-[140px]">
            <div class="flex flex-col items-center gap-2">
              <div class="text-sm font-medium text-white mb-2">{{ option2Name }}</div>
              <div class="flex gap-2">
                <!-- 수락한 사용자 아이콘들 -->
                <div 
                  v-for="i in stanceCounts.option2.accept" 
                  :key="`accepted-option2-${i}`"
                  class="flex flex-col items-center gap-1"
                >
                  <img 
                    :src="getUserIcon('option2')" 
                    class="w-24 h-24 rounded-full"
                    :alt="getUserIconAlt('option2')"
                  />
                  <span class="text-xs text-white">수락</span>
                </div>
                
                <!-- 대기 중인 사용자 아이콘들 -->
                <div 
                  v-for="i in stanceCounts.option2.waiting" 
                  :key="`waiting-option2-${i}`"
                  class="flex flex-col items-center gap-1"
                >
                  <img 
                    :src="waitingIcon" 
                    class="w-24 h-24 rounded-full"
                    alt="대기 중"
                  />
                  <span class="text-xs text-white">대기 중</span>
                </div>
                
                <!-- 거절한 사용자 아이콘들 -->
                <div 
                  v-for="i in stanceCounts.option2.reject" 
                  :key="`rejected-option2-${i}`"
                  class="flex flex-col items-center gap-1"
                >
                  <img 
                    :src="rejectIcon" 
                    class="w-24 h-24 rounded-full"
                    alt="거절"
                  />
                  <span class="text-xs text-white">거절</span>
                </div>
              </div>
            </div>
          </Card>
        </div>
        
        <!-- 연결 상태 표시 -->
        <div class="text-center">
          <p class="text-sm text-muted-foreground">
            연결된 사용자: {{ stanceCounts.option1.accept + stanceCounts.option1.reject + stanceCounts.option2.accept + stanceCounts.option2.reject }}/{{ totalCount }}
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
        <div class="flex justify-center">
          <div class="relative w-24 h-24">
            <!-- 원형 프로그레스바 -->
            <svg class="w-full h-full" viewBox="0 0 100 100">
              <!-- 배경 원 -->
              <circle 
                cx="50" 
                cy="50" 
                r="45" 
                fill="none" 
                stroke="#e5e7eb" 
                stroke-width="8"
              />
              <!-- 진행률 원 -->
              <circle 
                cx="50" 
                cy="50" 
                r="45" 
                fill="none" 
                stroke="#3b82f6" 
                stroke-width="8"
                stroke-linecap="round"
                :stroke-dasharray="`${(15 - timeLeft) / 15 * 283} 283`"
                transform="rotate(-90 50 50)"
                class="transition-all duration-1000"
              />
            </svg>
            
            <!-- 중앙 시간 표시 -->
            <div class="absolute inset-0 flex flex-col items-center justify-center">
              <div class="text-lg font-bold text-blue-600">{{ timeLeft }}</div>
              <div class="text-xs text-muted-foreground">초</div>
            </div>
            
            <!-- 움직이는 run 아이콘 -->
            <img 
              :src="runIcon" 
              class="absolute w-12 h-12 animate-pulse"
              :style="{ 
                left: `${50 + (40 * Math.cos((15 - timeLeft) / 15 * 2 * Math.PI - Math.PI/2))}%`,
                top: `${50 + (40 * Math.sin((15 - timeLeft) / 15 * 2 * Math.PI - Math.PI/2))}%`,
                transform: `translate(-50%, -50%) rotate(${(15 - timeLeft) / 15 * 360}deg)`
              }"
              alt="진행 중"
            />
          </div>
        </div>
      </div>
      
      <!-- 버튼들 -->
      <div class="flex gap-4">
        <Button 
          v-if="!isConnecting && !hasAccepted && !hasRejected"
          @click="handleAccept"
          class="flex-1 w-full bg-debate-random hover:bg-debate-random/90 text-white"
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
          v-if="!isConnecting && !hasAccepted && !hasRejected"
          @click="handleReject"
          class="flex-1 bg-white text-red-600 border border-red-600 hover:bg-red-50"
          size="lg"
        >
          거절
        </Button>
        <div 
          v-else-if="hasRejected"
          class="flex-1 flex items-center justify-center bg-white text-red-600 border border-red-600 rounded-lg px-4 py-3 text-lg font-medium"
        >
          매칭 거절
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { UserIcon } from 'lucide-vue-next'

import { Card } from '@/components/ui/card'
import { useTopicSetStore } from '@/store/topicSet'
import { useAuthStore } from '@/store/auth'
import { useMatchingModals } from '@/composables/useMatchingModals'
import debateLeftIcon from '@/assets/images/profile/debate_left.png'
import debateRightIcon from '@/assets/images/profile/debate_right.png'
import debateRandomIcon from '@/assets/images/profile/debate_random.png'
import waitingIcon from '@/assets/images/profile/waiting.png'
import rejectIcon from '@/assets/images/profile/reject.png'
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
  timeLeft: 15,
  isConnecting: false,
  lastAcceptedStance: undefined
})
const emit = defineEmits<Emits>()

// isOpen을 반응형으로 만들기 (수락/거절 후에는 닫지 않음)
const isOpen = computed({
  get: () => props.isOpen,
  set: (value) => {
    // 수락하거나 거절한 상태가 아닐 때만 모달을 닫을 수 있음
    if (!value && !localHasAccepted.value && !localHasRejected.value) {
      emit('update:isOpen', value)
    }
  }
})

// 주제 정보 가져오기
const topicSetStore = useTopicSetStore()
const authStore = useAuthStore()
const modals = useMatchingModals()

// 현재 사용자 아이디
const currentUserId = computed(() => authStore.user?.id || '')

// 현재 사용자가 수락했는지 확인 (로컬 상태 사용)
const localHasAccepted = ref(false)
// 현재 사용자가 거절했는지 확인 (로컬 상태 사용)
const localHasRejected = ref(false)

const hasAccepted = computed(() => {
  return localHasAccepted.value
})

const hasRejected = computed(() => {
  return localHasRejected.value
})

// 모달이 열릴 때 로컬 상태 초기화
watch(() => props.isOpen, (isOpen) => {
  if (isOpen) {
    localHasAccepted.value = false
    localHasRejected.value = false
    // 모달이 열릴 때 stanceCounts 초기화 (computed로 자동 계산됨)
  }
})

// 타이머가 끝났을 때 거절 상태로 설정
watch(() => props.timeLeft, (timeLeft) => {
  if (timeLeft === 0 && !localHasAccepted.value && !localHasRejected.value) {
    localHasRejected.value = true
  }
})

// props.lastAcceptedStance 변화 감지하여 카운트 업데이트
// watch(() => props.lastAcceptedStance, (newStance, oldStance) => {
//   if (newStance && (newStance === 'option1' || newStance === 'option2')) {
//     const beforeCount = acceptedCounts.value[newStance as 'option1' | 'option2']
//     acceptedCounts.value[newStance as 'option1' | 'option2']++
//     const afterCount = acceptedCounts.value[newStance as 'option1' | 'option2']
//   }
// })

const currentTopic = computed(() => {
  // topicId가 유효한지 확인
  if (!props.topicId || props.topicId <= 0) {
    console.warn('⚠️ 유효하지 않은 topicId:', props.topicId)
    return null
  }
  
  // 토픽 ID 타입 변환 (string -> number 또는 number -> number)
  const topicId = typeof props.topicId === 'string' ? parseInt(props.topicId) : props.topicId
  
  const topic = topicSetStore.currentSet?.topics.find(topic => topic.id === topicId)
  
  if (!topic) {
    console.warn('⚠️ topicId로 주제를 찾을 수 없음:', {
      originalTopicId: props.topicId,
      convertedTopicId: topicId,
      availableTopics: topicSetStore.currentSet?.topics.map(t => ({ id: t.id, title: t.title })) || []
    })
  }
  
  return topic
})

// 선택지 이름 가져오기 (fallback 로직 추가)
const option1Name = computed(() => {
  if (currentTopic.value?.option1) {
    return currentTopic.value.option1
  }
  
  // fallback: props.stanceText를 기반으로 기본값 제공
  if (props.stanceText === 'option1') {
    return '찬성'
  } else if (props.stanceText === 'option2') {
    return '반대'
  }
  
  return '선택1'
})

const option2Name = computed(() => {
  if (currentTopic.value?.option2) {
    return currentTopic.value.option2
  }
  
  // fallback: props.stanceText를 기반으로 기본값 제공
  if (props.stanceText === 'option1') {
    return '반대'
  } else if (props.stanceText === 'option2') {
    return '찬성'
  }
  
  return '선택2'
})

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

// 각 진영별 수락/거절/대기 현황 계산
const stanceCounts = computed(() => {
  const counts = {
    option1: {
      accept: modals.stanceAcceptance.value.option1.accept,
      reject: modals.stanceAcceptance.value.option1.reject,
      waiting: iconCount.value.left - modals.stanceAcceptance.value.option1.accept - modals.stanceAcceptance.value.option1.reject
    },
    option2: {
      accept: modals.stanceAcceptance.value.option2.accept,
      reject: modals.stanceAcceptance.value.option2.reject,
      waiting: iconCount.value.right - modals.stanceAcceptance.value.option2.accept - modals.stanceAcceptance.value.option2.reject
    }
  }
  
  return counts
})

const getStanceButtonClass = (stance: string) => {
  if (stance === 'option1') {
    return 'bg-debate-left text-slate-800 hover:bg-debate-left/90'
  } else if (stance === 'option2') {
    return 'bg-debate-right text-white hover:bg-debate-right/90'
  } else {
    return 'bg-gray-100 text-gray-800 hover:bg-gray-200'
  }
}

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
  // 로컬 상태를 true로 설정
  localHasAccepted.value = true
  
  // 수락 이벤트만 emit하고 모달은 닫지 않음
  emit('accept')
}

const handleReject = () => {
  // 로컬 상태를 true로 설정
  localHasRejected.value = true
  
  // 거절 이벤트만 emit하고 모달은 닫지 않음
  emit('reject')
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


</script> 