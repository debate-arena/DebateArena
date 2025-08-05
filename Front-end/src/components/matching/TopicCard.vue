<template>
  <div class="p-4 border border-border rounded-lg bg-card min-h-[180px] flex flex-col justify-center" :class="{ 'opacity-60': !hasIndividualSelection }">
    <h3 class="text-2xl font-semibold text-foreground mb-6 text-center" :class="{ 'text-muted-foreground': !hasIndividualSelection }">{{ topic.title }}</h3>
    
         <!-- 진영 선택 -->
     <div class="flex gap-3">
       <Button 
         variant="outline"
         size="lg"
         @click="handleStanceSelect('option1')"
         class="flex-1 h-12"
         :class="{ 
           'bg-debate-left hover:bg-debate-left/90 text-slate-800': getStanceVariant('option1') === 'default',
           'bg-background hover:bg-accent': getStanceVariant('option1') === 'outline'
         }"
       >
         <img v-if="getStanceVariant('option1') === 'default'" :src="debateLeftIcon" class="w-6 h-6 mr-2" alt="viking" />
         {{ topic.option1 }}
       </Button>
       <Button 
         variant="outline"
         size="lg"
         @click="handleStanceSelect('option2')"
         class="flex-1 h-12"
         :class="{ 
           'bg-debate-right hover:bg-debate-right/90 text-white': getStanceVariant('option2') === 'default',
           'bg-background hover:bg-accent': getStanceVariant('option2') === 'outline'
         }"
       >
         <img v-if="getStanceVariant('option2') === 'default'" :src="debateRightIcon" class="w-6 h-6 mr-2" alt="gladiator" />
         {{ topic.option2 }}
       </Button>
       <Button 
         variant="outline"
         size="lg"
         @click="handleStanceSelect('random')"
         class="flex-1 h-12"
         :class="{ 
           'bg-debate-random hover:bg-debate-random/90 text-slate-700': getStanceVariant('random') === 'default',
           'bg-background hover:bg-accent': getStanceVariant('random') === 'outline'
         }"
       >
         <img 
           v-if="getStanceVariant('random') === 'default'" 
           :src="debateRandomIcon" 
           class="w-6 h-6 mr-2 transition-all duration-300" 
           alt="dice" 
         />
         상관없음
       </Button>
     </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
// 아이콘 import
import debateLeftIcon from '@/assets/images/profile/debate_left.png'
import debateRightIcon from '@/assets/images/profile/debate_right.png'
import debateRandomIcon from '@/assets/images/profile/debate_random.png'
import { useMatchingStore } from '@/store/matching'
import { useThemeStore } from '@/store/theme'
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
const themeStore = useThemeStore()

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