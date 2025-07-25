<template>
  <div class="p-4 space-y-4">
    <h1 class="text-2xl font-bold mb-2">상태관리 테스트 UI</h1>
    <div class="flex gap-2 items-center">
      <span class="font-semibold">상태:</span>
      <span class="px-2 py-1 rounded bg-gray-100">{{ topicSetStore.status }}</span>
      <span v-if="topicSetStore.isLoading" class="text-blue-500">(로딩중)</span>
      <span v-if="topicSetStore.isSwapping" class="text-purple-500">(스왑중)</span>
      <span v-if="topicSetStore.isError" class="text-red-500">(에러)</span>
      <span v-if="topicSetStore.isReady" class="text-green-600">(READY)</span>
    </div>
    <div class="flex gap-2">
      <button class="px-3 py-1 bg-blue-500 text-white rounded" @click="topicSetStore.fetchTopicSets()">fetch</button>
      <button class="px-3 py-1 bg-purple-500 text-white rounded" @click="topicSetStore.swapSets()">swap</button>
      <button class="px-3 py-1 bg-gray-500 text-white rounded" @click="topicSetStore.reSyncServerTime()">reSync</button>
    </div>
    <div v-if="topicSetStore.error" class="text-red-600">에러: {{ topicSetStore.error }}</div>
    <div class="mt-4">
      <h2 class="font-semibold">현재 세트(currentSet)</h2>
      <pre class="bg-gray-50 p-2 rounded text-xs">{{ topicSetStore.currentSet }}</pre>
    </div>
    <div class="mt-2">
      <h2 class="font-semibold">다음 세트(nextSet)</h2>
      <pre class="bg-gray-50 p-2 rounded text-xs">{{ topicSetStore.nextSet }}</pre>
    </div>
    <div class="mt-2 text-xs text-gray-400">서버-클라이언트 시간 오프셋(ms): {{ topicSetStore.serverTimeOffsetMs }}</div>
    <div class="mt-2 text-lg font-semibold">
      세트 교체까지 남은 시간: <span class="text-blue-600">{{ formatMs(remainingMs) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useTopicSetStore } from '@/store/topicSet'
import { getRemainingMs } from '@/utils/topicSet'
import { useTopicSetController } from '@/composables/useTopicSetController'
useTopicSetController()

const topicSetStore = useTopicSetStore()

// 1초마다 갱신되는 now ref 추가
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null
onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// computed가 now.value를 의존하도록 수정
const serverNowMs = computed(() => now.value + topicSetStore.serverTimeOffsetMs)
const remainingMs = computed(() =>
  topicSetStore.currentSet
    ? getRemainingMs(topicSetStore.currentSet.endAtMs, serverNowMs.value)
    : 0
)
// mm:ss 포맷 변환 함수
function formatMs(ms: number) {
  const totalSec = Math.floor(ms / 1000)
  const min = Math.floor(totalSec / 60)
  const sec = totalSec % 60
  return `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
}
</script> 