<template>
  <div class="p-4 border border-border rounded-lg bg-card min-h-[180px] flex flex-col justify-center" :class="{ 'opacity-60': !hasIndividualSelection }">
    <div class="space-y-4">
      <!-- 주제 제목 -->
      <div class="text-center">
        <h3 class="text-2xl font-semibold text-foreground" :class="{ 'text-muted-foreground': !hasIndividualSelection }">{{ topic.title }}</h3>
      </div>
      
      <!-- 진영 선택 -->
      <div class="space-y-2">
        <div class="flex gap-3">
          <Button 
            variant="outline"
            size="lg"
            @click="handleStanceSelect('option1')"
            class="flex-1 h-12"
            :class="{ 
              'bg-debate-left hover:bg-debate-left/90 text-slate-800 border-debate-left': getStanceVariant('option1') === 'default',
              'bg-background hover:bg-accent': getStanceVariant('option1') === 'outline'
            }"
          >
            <img src="/src/assets/images/profile/debate_left.png" class="w-6 h-6 mr-2 polar-icon" alt="북극곰" />
            {{ topic.option1 }}
          </Button>
          
          <Button 
            variant="outline"
            size="lg"
            @click="handleStanceSelect('random')"
            class="flex-1 h-12"
            :class="{ 
              'bg-debate-random hover:bg-debate-random/90 text-slate-700 border-debate-random': getStanceVariant('random') === 'default',
              'bg-background hover:bg-accent': getStanceVariant('random') === 'outline'
            }"
          >
            <img src="/src/assets/images/profile/debate_random.png" class="w-6 h-6 mr-2 polar-icon" alt="물범" />
            상관없음
          </Button>
          
          <Button 
            variant="outline"
            size="lg"
            @click="handleStanceSelect('option2')"
            class="flex-1 h-12"
            :class="{ 
              'bg-debate-right hover:bg-debate-right/90 text-white border-debate-right': getStanceVariant('option2') === 'default',
              'bg-background hover:bg-accent': getStanceVariant('option2') === 'outline'
            }"
          >
            <img src="/src/assets/images/profile/debate_right.png" class="w-6 h-6 mr-2 polar-icon" alt="펭귄" />
            {{ topic.option2 }}
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import { useMatchingStore } from '@/store/matching'
import type { Topic } from '@/types/topic'
import type { Stance } from '@/types/matching'

interface Props {
  topic: Topic
}

interface Emits {
  (e: 'stance-select', topicId: number, stance: Stance): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const matchingStore = useMatchingStore()

// 진영 버튼 스타일 결정 (개별 선택 우선)
const getStanceVariant = (stance: Stance) => {
  const selection = matchingStore.topicSelections.get(props.topic.id)
  return selection?.stance === stance ? 'default' : 'outline'
}

// 진영 선택 핸들러
const handleStanceSelect = (stance: Stance) => {
  const selection = matchingStore.topicSelections.get(props.topic.id)
  // 같은 진영을 다시 누르면 선택 해제
  if (selection?.stance === stance) {
    matchingStore.removeTopicSelection(props.topic.id)
  } else {
    matchingStore.selectTopicStance(props.topic.id, stance)
  }
  emit('stance-select', props.topic.id, stance)
}

// 글로벌 상태에 따른 초기 선택 상태 확인
const hasIndividualSelection = computed(() => {
  return matchingStore.topicSelections.has(props.topic.id)
})
</script> 