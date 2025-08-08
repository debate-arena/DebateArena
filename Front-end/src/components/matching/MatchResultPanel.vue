<template>
  <!-- 매칭 성사 패널 - 전체 화면 원형 타이머 디자인 -->
  <div class="mt-8 w-full flex items-center justify-center bg-background/95 backdrop-blur-sm rounded-lg py-8">
    <!-- 거대한 원형 타이머 배경 -->
    <div class="relative w-[70vmin] h-[70vmin] max-w-[500px] max-h-[500px] flex items-center justify-center">
      <!-- SVG 원형 타이머 -->
      <svg 
        class="absolute inset-0 w-full h-full transform -rotate-90" 
        viewBox="0 0 200 200"
      >
        <!-- 배경 원 -->
        <circle
          cx="100"
          cy="100"
          r="90"
          fill="none"
          stroke="currentColor"
          stroke-width="8"
          class="text-foreground/10"
        />
        <!-- 진행 원 -->
        <circle
          cx="100"
          cy="100"
          r="90"
          fill="none"
          stroke="currentColor"
          stroke-width="8"
          stroke-linecap="round"
          class="text-foreground"
          :stroke-dasharray="circumference"
          :stroke-dashoffset="progressOffset"
          style="transition: stroke-dashoffset 1s linear;"
        />
      </svg>
      
      <!-- 움직이는 run 아이콘 -->
      <img 
        :src="runIcon" 
        class="absolute w-16 h-16 animate-pulse polar-icon polar-glow"
        :style="{ 
          left: `${50 + (47 * Math.cos((15 - (props.timeLeft ?? 15)) / 15 * 2 * Math.PI - Math.PI/2))}%`,
          top: `${50 + (47 * Math.sin((15 - (props.timeLeft ?? 15)) / 15 * 2 * Math.PI - Math.PI/2))}%`,
          transform: `translate(-50%, -50%) rotate(${(15 - (props.timeLeft ?? 15)) / 15 * 360}deg)`
        }"
        alt="진행 중"
      />
      
      <!-- 원 중앙의 콘텐츠 -->
      <div class="relative z-10 w-[70%] h-[70%] flex flex-col items-center justify-center text-center space-y-4">
        <!-- 타이머 숫자 -->
        <div class="text-6xl font-bold text-foreground font-mono">
          {{ props.timeLeft ?? 15 }}
        </div>
        
        <!-- 제목 -->
        <div class="space-y-2">
          <h3 class="text-2xl font-bold text-slate-800 dark:text-slate-100 flex items-center justify-center gap-2">
            ❄️ 매칭 성사!
          </h3>
          <p class="text-lg text-slate-600 dark:text-slate-300">
            수락하시겠습니까?
          </p>
        </div>
        
        <!-- 주제 정보 -->
        <div class="space-y-3">
          <div class="text-lg font-semibold text-foreground">
            {{ currentTopic?.title || props.topicTitle || '주제' }}
          </div>
          <div class="flex items-center justify-center gap-4">
            <div class="flex items-center gap-2">
              <span class="text-sm text-muted-foreground">선택:</span>
              <div 
                class="px-3 py-1 rounded-full text-sm font-medium"
                :class="getStanceBadgeClass(props.stance as Stance)"
              >
                {{ (() => {
                  const stance = props.stance as Stance
                  const stanceText = getStanceText(stance, currentTopic || undefined)
                  console.log('🎯 getStanceText 호출:', {
                    stance: stance,
                    propsStance: props.stance,
                    actualDataStance: props.stance,
                    topic: currentTopic,
                    topicOption1: currentTopic?.option1,
                    topicOption2: currentTopic?.option2,
                    result: stanceText
                  })
                  return stanceText
                })() }}
              </div>
            </div>
            <div class="flex items-center gap-2">
              <span class="text-sm text-muted-foreground">모드:</span>
              <div 
                class="px-3 py-1 rounded-full text-sm font-medium"
                :class="getModeBadgeClass(props.mode as PlayerMode)"
              >
                {{ props.mode ?? '1:1' }}
              </div>
            </div>
          </div>
        </div>
        
        <!-- 참가자 상태 (다이나믹 아이콘) -->
        <div class="flex items-center justify-center gap-6">
          <!-- 팀1 -->
          <div class="flex flex-col items-center gap-2">
            <div class="text-sm font-medium text-slate-800 dark:text-slate-200">{{ option1Name }}</div>
            <div class="flex gap-1">
              <!-- 수락한 사용자들 -->
              <div 
                v-for="i in stanceCounts.option1.accept" 
                :key="`accept-option1-${i}`"
                class="flex flex-col items-center gap-1"
              >
                <img 
                  :src="getUserIcon('option1')" 
                  class="w-8 h-8 rounded-full ring-2 ring-cyan-400 polar-icon polar-glow"
                  :alt="getUserIconAlt('option1')"
                />
                <span class="text-xs text-cyan-600">수락</span>
              </div>
              
              <!-- 거절한 사용자들 -->
              <div 
                v-for="i in stanceCounts.option1.reject" 
                :key="`reject-option1-${i}`"
                class="flex flex-col items-center gap-1"
              >
                <img 
                  :src="rejectIcon" 
                  class="w-8 h-8 rounded-full ring-2 ring-red-400 polar-icon"
                  alt="거절"
                />
                <span class="text-xs text-red-600">거절</span>
              </div>
              
              <!-- 대기 중인 사용자들 -->
              <div 
                v-for="i in stanceCounts.option1.waiting" 
                :key="`waiting-option1-${i}`"
                class="flex flex-col items-center gap-1"
              >
                <img 
                  :src="waitingIcon" 
                  class="w-8 h-8 rounded-full ring-2 ring-cyan-400 polar-icon"
                  alt="대기 중"
                />
                <span class="text-xs text-slate-500">대기</span>
              </div>
            </div>
          </div>
          
          <!-- VS -->
          <div class="text-xl font-bold text-foreground/60">VS</div>
          
          <!-- 팀2 -->
          <div class="flex flex-col items-center gap-2">
            <div class="text-sm font-medium text-slate-800 dark:text-slate-200">{{ option2Name }}</div>
            <div class="flex gap-1">
              <!-- 수락한 사용자들 -->
              <div 
                v-for="i in stanceCounts.option2.accept" 
                :key="`accept-option2-${i}`"
                class="flex flex-col items-center gap-1"
              >
                <img 
                  :src="getUserIcon('option2')" 
                  class="w-8 h-8 rounded-full ring-2 ring-cyan-400 polar-icon polar-glow"
                  :alt="getUserIconAlt('option2')"
                />
                <span class="text-xs text-cyan-600">수락</span>
              </div>
              
              <!-- 거절한 사용자들 -->
              <div 
                v-for="i in stanceCounts.option2.reject" 
                :key="`reject-option2-${i}`"
                class="flex flex-col items-center gap-1"
              >
                <img 
                  :src="rejectIcon" 
                  class="w-8 h-8 rounded-full ring-2 ring-red-400 polar-icon"
                  alt="거절"
                />
                <span class="text-xs text-red-600">거절</span>
              </div>
              
              <!-- 대기 중인 사용자들 -->
              <div 
                v-for="i in stanceCounts.option2.waiting" 
                :key="`waiting-option2-${i}`"
                class="flex flex-col items-center gap-1"
              >
                <img 
                  :src="waitingIcon" 
                  class="w-8 h-8 rounded-full ring-2 ring-cyan-400 polar-icon"
                  alt="대기 중"
                />
                <span class="text-xs text-slate-500">대기</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 액션 버튼들 -->
        <div class="flex gap-4 mt-6">
          <Button 
            v-if="!hasAccepted && !hasRejected"
            @click="handleAccept"
            size="lg"
            class="px-8 py-3 bg-cyan-600 hover:bg-cyan-700 text-white dark:bg-cyan-500 dark:hover:bg-cyan-600"
          >
            ✅ 수락
          </Button>
          <div 
            v-else-if="hasAccepted"
            class="px-8 py-3 bg-cyan-100 text-cyan-800 border border-cyan-300 rounded-lg text-lg font-medium flex items-center gap-2"
          >
            ✅ 수락 완료
          </div>
          
          <Button 
            v-if="!hasAccepted && !hasRejected"
            @click="handleReject"
            variant="outline"
            size="lg"
            class="px-8 py-3 text-slate-600 border-slate-300 hover:bg-slate-100 dark:text-slate-300 dark:border-slate-600 dark:hover:bg-slate-800"
          >
            ❌ 거절
          </Button>
          <div 
            v-else-if="hasRejected"
            class="px-8 py-3 bg-red-100 text-red-800 border border-red-300 rounded-lg text-lg font-medium flex items-center gap-2"
          >
            ❌ 거절 완료
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { Button } from '@/components/ui/button'
import { useTopicSetStore } from '@/store/topicSet'
import type { Stance, PlayerMode } from '@/types/matching'

import { getTotalCount, getStanceText, getStanceBadgeClass, getModeBadgeClass } from '@/utils/matching'

// Props
interface Props {
  isOpen: boolean
  topicTitle: string
  // 사용자의 선택 키값 (option1 | option2 | random)
  stance: string

  // 사용자 본인의 수락 상태 (버튼 UI만 제어)
  selfAcceptance?: 'pending' | 'accepted' | 'rejected'
  mode: string
  topicId: number
  totalCount: number
  timeLeft: number
  isConnecting: boolean
  lastAcceptedStance?: string
  stanceAcceptance?: {
    option1: { accept: number; reject: number }
    option2: { accept: number; reject: number }
  }
}

const props = withDefaults(defineProps<Props>(), {
  stanceAcceptance: () => ({ 
    option1: { accept: 0, reject: 0 }, 
    option2: { accept: 0, reject: 0 } 
  }),
  selfAcceptance: 'pending'
})

// Emits
const emit = defineEmits<{
  accept: []
  reject: []
  'update:is-open': [value: boolean]
}>()

// 로컬 수락/거절 상태 관리
const hasAccepted = ref(false)
const hasRejected = ref(false)

// 주제 정보 가져오기
const topicSetStore = useTopicSetStore()

// 사용자 진영 확인 (키값 기반)
const userStance = computed(() => props.stance)

// 진영별 수락/거절 카운트 (사용자 상태 반영)
const baseStanceCounts = {
  option1: { accept: 0, reject: 0, waiting: 0 },
  option2: { accept: 0, reject: 0, waiting: 0 }
}

// 가시성 제어
const isVisible = computed(() => props.isOpen)

// 현재 주제 정보
const currentTopic = computed(() => {
  // topicId가 유효하지 않으면 null 반환
  if (!props.topicId || props.topicId === 0) {
    console.log('⚠️ currentTopic: topicId가 유효하지 않음:', props.topicId)
    return null
  }
  
  // topicSetStore가 아직 로드되지 않았으면 null 반환
  if (!topicSetStore.currentSet) {
    console.log('⚠️ currentTopic: topicSetStore.currentSet이 null')
    return null
  }
  
  // topicSetStore.currentSet.topics가 없으면 null 반환
  if (!topicSetStore.currentSet.topics) {
    console.log('⚠️ currentTopic: topicSetStore.currentSet.topics가 null')
    return null
  }
  
  const topic = topicSetStore.currentSet.topics.find(topic => topic.id === props.topicId)
  
  console.log('🔍 currentTopic computed:', {
    topicId: props.topicId,
    foundTopic: topic,
    availableTopics: topicSetStore.currentSet.topics.map(t => ({ id: t.id, title: t.title, option1: t.option1, option2: t.option2 })),
    topicSetStatus: topicSetStore.status
  })
  
  return topic
})

const option1Name = computed(() => currentTopic.value?.option1 || '찬성')
const option2Name = computed(() => currentTopic.value?.option2 || '반대')

// 진영별 수락/거절 카운트 (서버 수치만 사용, 로컬은 버튼 UI만 제어)
const stanceCounts = computed(() => {
  const counts = JSON.parse(JSON.stringify(baseStanceCounts)) // 깊은 복사
  const teamSize = getTeamSize()
  
  // 서버에서 받은 다른 사용자들의 상태 반영 (props로 전달됨)
  const serverAcceptance = props.stanceAcceptance || { option1: { accept: 0, reject: 0 }, option2: { accept: 0, reject: 0 } }
  counts.option1.accept = serverAcceptance.option1.accept
  counts.option1.reject = serverAcceptance.option1.reject
  counts.option2.accept = serverAcceptance.option2.accept
  counts.option2.reject = serverAcceptance.option2.reject
  
  // 대기 인원 계산 (전체 - 수락 - 거절)
  counts.option1.waiting = Math.max(0, teamSize - counts.option1.accept - counts.option1.reject)
  counts.option2.waiting = Math.max(0, teamSize - counts.option2.accept - counts.option2.reject)
  
  console.log('🎯 stanceCounts 계산됨:', { 
    serverAcceptance, 
    userStance: userStance.value,
    hasAccepted: hasAccepted.value,
    hasRejected: hasRejected.value,
    finalCounts: counts 
  })
  
  return counts
})

// 서버/부모에서 전달된 내 수락 상태에 따라 버튼 UI 갱신
watch(
  () => props.selfAcceptance,
  (val) => {
    if (val === 'accepted') {
      hasAccepted.value = true
      hasRejected.value = false
    } else if (val === 'rejected') {
      hasAccepted.value = false
      hasRejected.value = true
    } else {
      hasAccepted.value = false
      hasRejected.value = false
    }
  },
  { immediate: true }
)

// 아이콘 경로들
const waitingIcon = '/src/assets/images/profile/waiting.png'
const rejectIcon = '/src/assets/images/profile/reject.png'
const runIcon = '/src/assets/images/profile/run.png'

const getUserIcon = (stance: string) => {
  if (stance === 'option1') {
    return '/src/assets/images/profile/debate_left.png'
  } else if (stance === 'option2') {
    return '/src/assets/images/profile/debate_right.png'
  }
  return waitingIcon
}

const getUserIconAlt = (stance: string) => {
  if (stance === 'option1') {
    return '북극곰'
  } else if (stance === 'option2') {
    return '펭귄'
  }
  return '대기 중'
}

// 원형 타이머 계산
const circumference = computed(() => 2 * Math.PI * 90) // r=90인 원의 둘레

const progressOffset = computed(() => {
  const progress = (15 - (props.timeLeft ?? 15)) / 15 // 진행률 (0~1) - 지나간 시간
  return circumference.value * (1 - progress) // 남은 부분만큼 offset
})

// 팀 크기 계산 (유틸 함수 재활용)
const getTeamSize = () => {
  return getTotalCount(props.mode as PlayerMode) / 2
}

// 로컬 태그 클래스 함수 제거하고 utils의 배지 클래스 사용

// 수락/거절 핸들러
const handleAccept = () => {
  hasAccepted.value = true
  emit('accept')
}

const handleReject = () => {
  hasRejected.value = true
  emit('reject')
}
</script>
