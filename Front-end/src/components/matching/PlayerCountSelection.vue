<template>
  <Card class="p-4">
    <div class="space-y-4">
      <h3 class="text-sm font-medium text-foreground">모든 주제에 적용됩니다</h3>
      
      <!-- 모드 선택 -->
      <div class="space-y-2">
        <h4 class="text-xs font-medium text-muted-foreground">인원수</h4>
        <div class="flex gap-2">
          <div class="flex items-center gap-1">
            <Checkbox 
              v-model="modelMode1"
              :class="{ 'ring-2 ring-black dark:ring-white': modelMode1 }"
            />
            <label class="text-sm">1:1</label>
          </div>
          
          <div class="flex items-center gap-1">
            <Checkbox 
              v-model="modelMode2"
              :class="{ 'ring-2 ring-black dark:ring-white': modelMode2 }"
            />
            <label class="text-sm">2:2</label>
          </div>
        </div>
      </div>
      
      <!-- 진영 선택 -->
      <div class="space-y-2">
        <h4 class="text-xs font-medium text-muted-foreground">진영</h4>
        <div class="flex gap-2">
          <div class="flex items-center gap-1">
            <Checkbox 
              v-model="modelStance1"
              :class="{ 'bg-debate-left ring-2 ring-black dark:ring-white': modelStance1 }"
            />
            <label class="text-sm flex items-center gap-1">
              <img v-if="modelStance1" :src="vikingIcon" class="w-4 h-4" alt="viking" />
              선택1
            </label>
          </div>
          
          <div class="flex items-center gap-1">
            <Checkbox 
              v-model="modelStance2"
              :class="{ 'bg-debate-right ring-2 ring-black dark:ring-white': modelStance2 }"
            />
            <label class="text-sm flex items-center gap-1">
              <img v-if="modelStance2" :src="gladiatorIcon" class="w-4 h-4" alt="gladiator" />
              선택2
            </label>
          </div>
          
          <div class="flex items-center gap-1">
            <Checkbox 
              v-model="modelStanceRandom"
              :class="{ 'ring-2 ring-gray-500': modelStanceRandom }"
            />
            <label class="text-sm flex items-center gap-1">
              <img 
                v-if="modelStanceRandom" 
                :src="currentDiceIcon" 
                class="w-4 h-4 transition-all duration-300" 
                alt="dice" 
              />
              상관없음
            </label>
          </div>
        </div>
      </div>
    </div>
  </Card>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { Card } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import vikingIcon from '@/assets/images/profile/viking.png'
import gladiatorIcon from '@/assets/images/profile/gladiator.png'
import diceIcon from '@/assets/images/profile/dice.png'
import diceDarkIcon from '@/assets/images/profile/dice-dark.png'
import { useMatchingStore } from '@/store/matching'
import { useThemeStore } from '@/store/theme'
import { storeToRefs } from 'pinia'
import type { Stance, PlayerMode } from '@/types/matching'

interface Emits {
  (e: 'global-mode-toggle', mode: PlayerMode): void
  (e: 'global-stance-toggle', stance: Stance): void
}

const emit = defineEmits<Emits>()
const matchingStore = useMatchingStore()
const themeStore = useThemeStore()
const { globalModes, globalStances } = storeToRefs(matchingStore)

// 다크모드에 따른 dice 아이콘 선택
const currentDiceIcon = computed(() => {
  return themeStore.isDark ? diceDarkIcon : diceIcon
})

// v-model용 computed getter/setter
const modelMode1 = computed({
  get: () => globalModes.value.has('1:1'),
  set: (val: boolean) => {
    if (val) {
      if (!globalModes.value.has('1:1')) matchingStore.toggleGlobalMode('1:1')
    } else {
      if (globalModes.value.has('1:1')) matchingStore.toggleGlobalMode('1:1')
    }
    emit('global-mode-toggle', '1:1')
  }
})
const modelMode2 = computed({
  get: () => globalModes.value.has('2:2'),
  set: (val: boolean) => {
    if (val) {
      if (!globalModes.value.has('2:2')) matchingStore.toggleGlobalMode('2:2')
    } else {
      if (globalModes.value.has('2:2')) matchingStore.toggleGlobalMode('2:2')
    }
    emit('global-mode-toggle', '2:2')
  }
})
const modelStance1 = computed({
  get: () => globalStances.value.has('option1'),
  set: (val: boolean) => {
    if (val) matchingStore.setGlobalStance('option1')
    emit('global-stance-toggle', 'option1')
  }
})
const modelStance2 = computed({
  get: () => globalStances.value.has('option2'),
  set: (val: boolean) => {
    if (val) matchingStore.setGlobalStance('option2')
    emit('global-stance-toggle', 'option2')
  }
})
const modelStanceRandom = computed({
  get: () => globalStances.value.has('random'),
  set: (val: boolean) => {
    if (val) matchingStore.setGlobalStance('random')
    emit('global-stance-toggle', 'random')
  }
})

// 디버깅용 watch
watch(globalModes, (val) => {
  console.log('글로벌 모드 변경:', Array.from(val))
}, { immediate: true })
watch(globalStances, (val) => {
  console.log('글로벌 진영 변경:', Array.from(val))
}, { immediate: true })

// 강제로 computed 값 업데이트를 위한 watch
watch(() => globalModes.value, () => {
  // computed 값이 변경되도록 강제 업데이트
}, { deep: true, immediate: true })

watch(() => globalStances.value, () => {
  // computed 값이 변경되도록 강제 업데이트
}, { deep: true, immediate: true })
</script> 