<template>
  <div class="min-h-screen bg-background">
    <div class="max-w-7xl mx-auto px-6 py-8">
      <div class="relative flex flex-col gap-6">
        <!-- 상단 화살표 + 자동 전환 (컴팩트, sticky) -->
        <div class="sticky top-2 z-10 flex items-center justify-center gap-4">
          <Button variant="ghost" size="sm" :disabled="totalTopics === 0" @click="prevTopic" title="이전 주제">←</Button>
          <Button variant="ghost" size="sm" :disabled="totalTopics === 0" @click="toggleAutoChange">
            {{ autoChangeInterval ? '자동 전환 중지' : '자동 전환 시작' }}
          </Button>
          <Button variant="ghost" size="sm" :disabled="totalTopics === 0" @click="nextTopic" title="다음 주제">→</Button>
        </div>

        <!-- 제목 영역 -->
        <div class="text-center">
          <h1 class="text-5xl font-bold text-foreground leading-snug break-words max-w-3xl mx-auto">
            {{ currentTopic?.title || '주제를 불러오는 중...' }}
          </h1>
          <!-- CTA 버튼들 -->
          <div class="mt-4 flex items-center justify-center gap-3">
            <Button variant="default" @click="$router.push('/matching')">게임하기</Button>
            <Button variant="outline">관전하기</Button>
          </div>
        </div>

        <!-- 구분선 -->
        <div class="h-px w-full bg-border/60"></div>

        <!-- 동물 아이콘 영역 -->
        <div class="flex items-center justify-center">
          <div class="w-full px-10 py-10">
            <!-- 하단 작은 아이콘 트리거 (좌/중앙/우: 북극곰 / 물범 / 펭귄) -->
            <div class="flex items-end justify-center gap-4">
              <!-- 왼쪽: 북극곰 (선택1) -->
              <div class="relative" @click="selectAnimal(0)">
                <div class="w-20 h-20 flex items-center justify-center transition-all duration-150 cursor-pointer"
                     :class="selectedAnimalIndex === 0 ? 'opacity-100 scale-110' : 'opacity-50 scale-100'">
                  <img :src="animals[0].avatar" :alt="animals[0].name" class="w-16 h-16" />
                </div>
                <!-- 높이 맞춤용 빈 박스 -->
                <div class="h-6"></div>
              </div>

              <!-- 중앙: 물범 (선택 불가, 라벨 없음) -->
              <div class="relative select-none cursor-default">
                <!-- 말풍선: 항상 표시 -->
                <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2">
                  <div class="bg-card text-card-foreground px-4 py-2 rounded-xl shadow-md border border-border text-sm">
                    <div class="flex items-center gap-2 whitespace-nowrap">
                      <span>선택1?</span>
                      <span>선택2?</span>
                    </div>
                  </div>
                  <div class="absolute top-full left-1/2 -translate-x-1/2 w-0 h-0 border-l-[8px] border-r-[8px] border-t-[8px] border-transparent" style="border-top-color: hsl(var(--card))"></div>
                </div>
                <div class="w-20 h-20 flex items-center justify-center opacity-90">
                  <img :src="animals[1].avatar" :alt="animals[1].name" class="w-16 h-16" />
                </div>
                <!-- 높이 맞춤용 빈 박스 -->
                <div class="h-6"></div>
              </div>

              <!-- 오른쪽: 펭귄 (선택2) -->
              <div class="relative" @click="selectAnimal(2)">
                <div class="w-20 h-20 flex items-center justify-center transition-all duration-150 cursor-pointer"
                     :class="selectedAnimalIndex === 2 ? 'opacity-100 scale-110' : 'opacity-50 scale-100'">
                  <img :src="animals[2].avatar" :alt="animals[2].name" class="w-16 h-16" />
                </div>
                <!-- 높이 맞춤용 빈 박스 -->
                <div class="h-6"></div>
              </div>
            </div>

            <!-- 아이콘 아래 프리뷰 영역 (3분할 그리드: 좌 아이콘 / 중앙 말풍선 / 우 아이콘) -->
            <div class="mt-16 grid grid-cols-[minmax(24rem,30rem)_auto_minmax(24rem,30rem)] items-center justify-items-center gap-4 min-h-[36rem]">
              <!-- 좌 아이콘 슬롯 -->
              <div class="flex items-center justify-center w-[28rem] h-[28rem]">
                <img
                  v-if="animals[selectedAnimalIndex].name === '북극곰'"
                  :src="animals[0].avatar"
                  :alt="animals[0].name"
                  class="h-[34rem] w-auto object-contain"
                />
              </div>

              <!-- 중앙 말풍선 -->
              <div class="relative">
                <div
                  class="bg-card text-card-foreground px-8 py-5 rounded-2xl shadow-2xl drop-shadow-md border-2 max-w-2xl text-left text-xl leading-snug chat-bubble whitespace-nowrap overflow-x-auto"
                  :class="[ getBubbleSideBorderClass(animals[selectedAnimalIndex].name), 'border-border', { 'chat-pop': justSpoke } ]"
                >
                  {{ getAnimalSpeech(animals[selectedAnimalIndex].name) }}
                </div>
                <!-- 꼬리: 아이콘 방향으로 표시 (두 겹) -->
                <div
                  v-if="animals[selectedAnimalIndex].name === '북극곰'"
                  class="absolute top-1/2 -translate-y-1/2 left-[-16px] w-0 h-0 drop-shadow"
                >
                  <div class="absolute -left-[2px] -translate-y-1/2 top-1/2 w-0 h-0 border-y-[14px] border-y-transparent border-r-[14px]" style="border-right-color: hsl(var(--border))"></div>
                  <div class="absolute left-0 -translate-y-1/2 top-1/2 w-0 h-0 border-y-[13px] border-y-transparent border-r-[13px]" style="border-right-color: hsl(var(--card))"></div>
                </div>
                <div
                  v-else
                  class="absolute top-1/2 -translate-y-1/2 right-[-16px] w-0 h-0 drop-shadow"
                >
                  <div class="absolute -right-[2px] -translate-y-1/2 top-1/2 w-0 h-0 border-y-[14px] border-y-transparent border-l-[14px]" style="border-left-color: hsl(var(--border))"></div>
                  <div class="absolute right-0 -translate-y-1/2 top-1/2 w-0 h-0 border-y-[13px] border-y-transparent border-l-[13px]" style="border-left-color: hsl(var(--card))"></div>
                </div>
              </div>

              <!-- 우 아이콘 슬롯 -->
              <div class="flex items-center justify-center w-[28rem] h-[28rem]">
                <img
                  v-if="animals[selectedAnimalIndex].name === '펭귄'"
                  :src="animals[2].avatar"
                  :alt="animals[2].name"
                  class="h-[34rem] w-auto object-contain"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Button } from '@/components/ui/button'
import { useTopicSetStore } from '@/store/topicSet'
// import type { Topic } from '@/types/topic'

// Store
const topicSetStore = useTopicSetStore()

// 상태 변수
const currentTopicIndex = ref(0)
const currentAnimalIndex = ref(0)
const selectedAnimalIndex = ref<number>(0)
const autoChangeInterval = ref<number | null>(null)
const justSpoke = ref(false)

// 동물 데이터
const animals = [
  { name: '북극곰', avatar: '/src/assets/images/profile/debate_left.png' },
  { name: '물범',  avatar: '/src/assets/images/profile/debate_random.png' },
  { name: '펭귄',  avatar: '/src/assets/images/profile/debate_right.png'  },
]

// 계산된 속성
const currentTopic = computed(() => {
  if (!topicSetStore.currentSet?.topics) return null
  return topicSetStore.currentSet.topics[currentTopicIndex.value] || null
})

const totalTopics = computed(() => topicSetStore.currentSet?.topics?.length || 0)

// 말풍선 텍스트
const getAnimalSpeech = (animalName: string): string => {
  if (animalName === '북극곰') return currentTopic.value?.option1 || '옵션 1'
  if (animalName === '펭귄') return currentTopic.value?.option2 || '옵션 2'
  return ''
}

// 메서드
const nextTopic = () => {
  if (totalTopics.value === 0) return
  currentTopicIndex.value = (currentTopicIndex.value + 1) % totalTopics.value
  nextAnimal()
  ensureSelected()
}

const prevTopic = () => {
  if (totalTopics.value === 0) return
  currentTopicIndex.value = (currentTopicIndex.value - 1 + totalTopics.value) % totalTopics.value
  nextAnimal()
  ensureSelected()
}

const nextAnimal = () => {
  currentAnimalIndex.value = (currentAnimalIndex.value + 1) % animals.length
}

const selectAnimal = (idx: number) => {
  selectedAnimalIndex.value = idx
  triggerSpeakPop()
}

const ensureSelected = () => {
  // 초기 진입 시 기본 선택 유지(북극곰)
  triggerSpeakPop()
}

// 자동 변경 타이머
const startAutoChange = () => {
  if (autoChangeInterval.value) return
  autoChangeInterval.value = setInterval(() => {
    nextTopic()
    triggerSpeakPop()
  }, 30000)
}

const stopAutoChange = () => {
  if (autoChangeInterval.value) {
    clearInterval(autoChangeInterval.value)
    autoChangeInterval.value = null
  }
}

const toggleAutoChange = () => {
  if (autoChangeInterval.value) stopAutoChange()
  else startAutoChange()
}

// 말풍선 팝 애니메이션 트리거
let popTimer: number | null = null
const triggerSpeakPop = () => {
  justSpoke.value = false
  requestAnimationFrame(() => {
    justSpoke.value = true
    if (popTimer) {
      clearTimeout(popTimer)
      popTimer = null
    }
    popTimer = setTimeout(() => {
      justSpoke.value = false
    }, 260) as unknown as number
  })
}

// 말풍선 본체 보더: 꼬리 방향 쪽 보더 제거(경계 자연스러움)
const getBubbleSideBorderClass = (animalName: string): string => {
  if (animalName === '북극곰') {
    return 'border-l-0'
  }
  // 펭귄
  return 'border-r-0'
}

// 생명주기
onMounted(async () => {
  await topicSetStore.fetchTopicSets()
  ensureSelected()
  startAutoChange()
})

onUnmounted(() => {
  stopAutoChange()
})
</script>

<style scoped>
@keyframes chatPopIn {
  0% {
    transform: translateY(10px) scale(0.96);
    opacity: 0;
  }
  60% {
    transform: translateY(0) scale(1.03);
    opacity: 1;
  }
  100% {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

.chat-bubble {
  transform-origin: bottom center;
}

.chat-pop {
  animation: chatPopIn 260ms cubic-bezier(0.22, 1, 0.36, 1);
}
</style> 