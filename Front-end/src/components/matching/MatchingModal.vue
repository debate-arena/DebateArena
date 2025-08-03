<template>
  <Dialog :open="isOpen" :modal="false">
    <DialogContent class="sm:max-w-2xl" :close-on-escape="true" :close-on-backdrop="true">
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
                <div 
                  v-for="(user, index) in users.filter(u => u.stance === 'option1')" 
                  :key="user.userId"
                  class="flex flex-col items-center gap-3"
                >
                  <img 
                    v-if="user.accept === true" 
                    :src="debateLeftIcon" 
                    class="w-12 h-12 rounded-full"
                    alt="찬성 아이콘"
                  />
                  <X 
                    v-else-if="user.accept === false" 
                    class="w-12 h-12 text-debate-random" 
                  />
                  <UserIcon 
                    v-else 
                    class="w-12 h-12 text-debate-random" 
                  />
                  <span class="text-base text-slate-700">{{ user.userId }}</span>
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
                <div 
                  v-for="(user, index) in users.filter(u => u.stance === 'option2')" 
                  :key="user.userId"
                  class="flex flex-col items-center gap-3"
                >
                  <img 
                    v-if="user.accept === true" 
                    :src="debateRightIcon" 
                    class="w-12 h-12 rounded-full"
                    alt="반대 아이콘"
                  />
                  <X 
                    v-else-if="user.accept === false" 
                    class="w-12 h-12 text-debate-random" 
                  />
                  <UserIcon 
                    v-else 
                    class="w-12 h-12 text-debate-random" 
                  />
                  <span class="text-base text-white">{{ user.userId }}</span>
                </div>
              </div>
            </div>
          </Card>
        </div>

        <div class="text-center text-sm text-muted-foreground">
          {{ connectedCount }}/{{ totalCount }} 명 연결됨
        </div>
      </div>
      
      <!-- 수락/거절 버튼 -->
      <div class="flex gap-2">
        <div 
          v-if="!isConnecting"
          @click="handleAccept"
          @mousedown="() => console.log('🎯 수락 버튼 마우스다운!')"
          @touchstart="() => console.log('🎯 수락 버튼 터치!')"
          @pointerdown="() => console.log('🎯 수락 버튼 포인터다운!')"
          class="flex-1 bg-debate-random text-slate-700 hover:bg-debate-random/90 border-debate-random px-4 py-2 rounded-md cursor-pointer text-center font-medium"
          style="pointer-events: auto !important; z-index: 9999 !important; position: relative;"
        >
          수락
        </div>
        <div 
          v-if="!isConnecting"
          @click="handleReject"
          @mousedown="() => console.log('🎯 거절 버튼 마우스다운!')"
          @touchstart="() => console.log('🎯 거절 버튼 터치!')"
          @pointerdown="() => console.log('🎯 거절 버튼 포인터다운!')"
          class="flex-1 bg-white text-black hover:bg-gray-100 border-gray-300 px-4 py-2 rounded-md cursor-pointer text-center font-medium"
          style="pointer-events: auto !important; z-index: 9999 !important; position: relative;"
        >
          거절
        </div>
        <div 
          v-if="isConnecting"
          class="flex-1 bg-debate-random text-slate-700 px-4 py-2 rounded-md text-center font-medium"
        >
          수락 완료
        </div>
      </div>
      
      <!-- 타이머 -->
      <div v-if="!isConnecting" class="mt-4 space-y-2">
        <div class="flex justify-between text-xs text-muted-foreground">
          <span>수락 제한 시간</span>
          <span>{{ Math.ceil(timeLeft) }}초</span>
        </div>
        <div class="w-full bg-gray-200 rounded-full h-2">
          <div 
            class="bg-debate-random h-2 rounded-full transition-all duration-200"
            :style="{ width: `${((10 - timeLeft) / 10) * 100}%` }"
          ></div>
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { UserIcon, X, Loader2 } from 'lucide-vue-next'
import type { User } from '@/types/modal'
import { Card } from '@/components/ui/card'
import { useTopicSetStore } from '@/store/topicSet'
import debateLeftIcon from '@/assets/images/profile/debate_left.png'
import debateRightIcon from '@/assets/images/profile/debate_right.png'

interface Props {
  isOpen: boolean
  topicTitle: string
  stanceText: string
  mode: string
  topicId: number
  users: User[]
  connectedCount: number
  totalCount: number
  timeLeft: number
  isConnecting: boolean
}

interface Emits {
  (e: 'accept'): void
  (e: 'reject'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 주제 정보 가져오기
const topicSetStore = useTopicSetStore()
const currentTopic = computed(() => {
  return topicSetStore.currentSet?.topics.find(topic => topic.id === props.topicId)
})

// 선택지 이름 가져오기
const option1Name = computed(() => currentTopic.value?.option1 || '선택1')
const option2Name = computed(() => currentTopic.value?.option2 || '선택2')

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
  console.log('🎯 MatchingModal - 수락 버튼 클릭됨!')
  console.log('🔍 현재 props:', props)
  console.log('🔍 isConnecting 상태:', props.isConnecting)
  console.log('🔍 버튼 disabled 상태:', props.isConnecting)
  
  try {
    emit('accept')
    console.log('✅ accept 이벤트 발생됨')
  } catch (error) {
    console.error('❌ accept 이벤트 발생 실패:', error)
  }
}

const handleReject = () => {
  console.log('🎯 MatchingModal - 거절 버튼 클릭됨!')
  console.log('🔍 현재 props:', props)
  console.log('🔍 isConnecting 상태:', props.isConnecting)
  console.log('🔍 버튼 disabled 상태:', props.isConnecting)
  
  try {
    emit('reject')
    console.log('✅ reject 이벤트 발생됨')
  } catch (error) {
    console.error('❌ reject 이벤트 발생 실패:', error)
  }
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