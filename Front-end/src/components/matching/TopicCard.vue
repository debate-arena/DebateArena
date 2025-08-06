<template>
  <div class="p-4 border border-border rounded-lg bg-card">
    <h4 class="font-medium text-foreground mb-2">{{ topic.title }}</h4>
    <p class="text-sm text-muted-foreground mb-3">{{ topic.option1 }} vs {{ topic.option2 }}</p>
    
    <!-- 진영 선택 -->
    <div class="flex gap-2 mb-3">
      <Button 
        :variant="getStanceVariant('option1')"
        size="sm"
        @click="handleStanceSelect('option1')"
      >
        {{ topic.option1 }}
      </Button>
      <Button 
        :variant="getStanceVariant('option2')"
        size="sm"
        @click="handleStanceSelect('option2')"
      >
        {{ topic.option2 }}
      </Button>
      <Button 
        :variant="getStanceVariant('random')"
        size="sm"
        @click="handleStanceSelect('random')"
      >
        상관없음
      </Button>
    </div>

    <!-- 모드 선택 -->
    <div class="flex gap-2">
      <Button 
        :variant="getModeVariant('1:1')"
        size="sm"
        @click="handleModeToggle('1:1')"
      >
        1:1
      </Button>
      <Button 
        :variant="getModeVariant('2:2')"
        size="sm"
        @click="handleModeToggle('2:2')"
      >
        2:2
      </Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import { useMatchingStore } from '@/store/matching'
import type { Topic } from '@/types/topic'
import type { Stance, PlayerMode } from '@/types/matching'

interface Props {
  topic: Topic
}

interface Emits {
  (e: 'stance-select', topicId: number, stance: Stance): void
  (e: 'mode-toggle', topicId: number, mode: PlayerMode): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const matchingStore = useMatchingStore()

// 진영 버튼 스타일 결정 (개별 선택 우선)
const getStanceVariant = (stance: Stance) => {
  const selection = matchingStore.topicSelections.get(props.topic.id)
  return selection?.stance === stance ? 'default' : 'outline'
}

// 모드 버튼 스타일 결정 (개별 선택 우선)
const getModeVariant = (mode: PlayerMode) => {
  const selection = matchingStore.topicSelections.get(props.topic.id)
  return selection?.modes.has(mode) ? 'default' : 'outline'
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

// 모드 선택/해제 핸들러
const handleModeToggle = (mode: PlayerMode) => {
  matchingStore.toggleTopicMode(props.topic.id, mode)
  emit('mode-toggle', props.topic.id, mode)
}

// 글로벌 상태에 따른 초기 선택 상태 확인
const hasIndividualSelection = computed(() => {
  return matchingStore.topicSelections.has(props.topic.id)
})
</script> 