<template>
  <div class="space-y-1">
    <!-- 모드 선택 -->
    <div class="space-y-2">
      <p class="text-xs text-muted-foreground text-center hidden">※ 최소 1개의 모드를 선택해주세요 ※</p>
      <div class="flex gap-3">
          <Button 
            variant="outline"
            @click="toggleMode1"
            class="px-5 h-12 min-w-[96px] text-base font-medium"
            :class="{ 
              'bg-mode-1v1 hover:bg-indigo-200 active:bg-indigo-300 text-mode-1v1 border-mode-1v1 ring-mode-1v1': modelMode1,
              'bg-background hover:bg-accent': !modelMode1
            }"
          >
            <img src="/src/assets/images/profile/1vs1.png" class="w-6 h-6 mr-2 polar-icon" alt="1대1" />
            1:1
          </Button>
          
          <Button 
            variant="outline"
            @click="toggleMode2"
            class="px-5 h-12 min-w-[96px] text-base font-medium"
            :class="{ 
              'bg-mode-2v2 hover:bg-indigo-200 active:bg-indigo-300 text-mode-2v2 border-mode-2v2 ring-mode-2v2': modelMode2,
              'bg-background hover:bg-accent': !modelMode2
            }"
          >
            <img src="/src/assets/images/profile/2vs2.png" class="w-6 h-6 mr-2 polar-icon" alt="2대2" />
            2:2
          </Button>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { useMatchingStore } from '@/store/matching'
import { storeToRefs } from 'pinia'
import type { PlayerMode } from '@/types/matching'

interface Emits {
  (e: 'global-mode-toggle', mode: PlayerMode): void
}

const emit = defineEmits<Emits>()
const matchingStore = useMatchingStore()
const { globalModes } = storeToRefs(matchingStore)

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

// 버튼 클릭 핸들러
const toggleMode1 = () => {
  modelMode1.value = !modelMode1.value
}

const toggleMode2 = () => {
  modelMode2.value = !modelMode2.value
}

// 디버깅용 watch
watch(globalModes, (val) => {
  console.log('글로벌 모드 변경:', Array.from(val))
}, { immediate: true })

// 강제로 computed 값 업데이트를 위한 watch
watch(() => globalModes.value, () => {
  // computed 값이 변경되도록 강제 업데이트
}, { deep: true, immediate: true })
</script> 