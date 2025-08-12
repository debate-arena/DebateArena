<template>
  <div class="bg-background">
    <!-- 이미지 히어로 섹션 -->
    <section class="relative mt-4 md:mt-6 h-[180px] md:h-[240px] overflow-hidden">
      <!-- 중앙 정보 -->
      <div class="relative z-10 h-full flex flex-col items-center justify-center gap-3 px-4">
        <!-- 상단 시간 표시: 주제 위, 호버 설명(물음표 아이콘) -->
        <div class="w-full max-w-5xl flex items-center justify-center gap-2 mb-1 md:mb-2">
          <div
            class="rounded-full border border-[hsl(var(--border))] bg-[hsl(var(--card))] text-[hsl(var(--card-foreground))] px-3 py-1 shadow-sm font-mono tabular-nums text-[18px] leading-6 md:text-[28px] md:leading-[34px]"
            aria-label="남은 시간"
          >
            {{ formattedRemainingTime }}
          </div>
          <Tooltip>
            <TooltipTrigger as-child>
              <HelpCircle class="w-4 h-4 md:w-5 md:h-5 text-foreground/80 cursor-help" aria-label="설명 보기" />
            </TooltipTrigger>
            <TooltipContent side="right" align="center" class="bg-[hsl(var(--card))] text-[hsl(var(--card-foreground))] border border-[hsl(var(--border))] tooltip-white">
              <p>이 시간대에는 5개의 주제가 제공되며, 1시간마다 새 주제로 교체됩니다.</p>
            </TooltipContent>
          </Tooltip>
        </div>

        <div class="w-full max-w-5xl flex items-center justify-center gap-2 md:gap-3">
          <Button
            variant="outline"
            size="icon"
            class="rounded-full shadow-lg bg-card text-foreground hover:bg-accent hover:text-accent-foreground size-12 md:size-14"
            :disabled="totalTopics === 0"
            @click="prevTopic"
            aria-label="이전 주제"
            title="이전 주제"
          >
            <ChevronLeft class="w-8 h-8 md:w-10 md:h-10" />
          </Button>
          <h1 class="flex-1 text-2xl md:text-5xl font-bold leading-tight tracking-tight text-foreground text-center">
            {{ currentTopic?.title || '주제를 불러오는 중...' }}
          </h1>
          <Button
            variant="outline"
            size="icon"
            class="rounded-full shadow-lg bg-card text-foreground hover:bg-accent hover:text-accent-foreground size-12 md:size-14"
            :disabled="totalTopics === 0"
            @click="nextTopic"
            aria-label="다음 주제"
            title="다음 주제"
          >
            <ChevronRight class="w-8 h-8 md:w-10 md:h-10" />
          </Button>
        </div>
        <!-- 모드 아이콘: 화살표 바로 아래 정렬 -->
        <div class="w-full max-w-5xl flex items-center justify-between mt-2 md:mt-3 px-2">
          <img src="@/assets/images/profile/1vs1.png" alt="1대1"
               class="w-14 md:w-20 opacity-85 select-none" />
          <img src="@/assets/images/profile/2vs2.png" alt="2대2"
               class="w-14 md:w-20 opacity-85 select-none" />
        </div>
        <!-- 아래 시간/다음 주제 표시는 제거 (요청) -->
      </div>
    </section>

    <!-- 본문 -->
    <div class="max-w-5xl mx-auto px-2 md:px-4 mt-4 md:mt-6 mb-16">
      <div class="relative flex items-end justify-center gap-6">
        <!-- 왼쪽: 북극곰 -->
        <div class="faction-card faction-bear" :class="{ 'is-selected': isLeftSelected }">
          <IconCell
            :src="animals[0].avatar"
            :selected="isLeftSelected"
            :badge-text="isLeftSelected ? leftOptionText : ''"
            @select="handleSelect('left')"
          />
        </div>

        <!-- 중앙: 물범(랜덤) - 배지 생략 -->
        <div class="faction-card faction-seal" :class="{ 'is-selected': false }">
          <IconCell
            :src="animals[1].avatar"
            :selected="false"
            @select="handleRandom()"
          />
        </div>

        <!-- 오른쪽: 펭귄 -->
        <div class="faction-card faction-peng" :class="{ 'is-selected': isRightSelected }">
          <IconCell
            :src="animals[2].avatar"
            :selected="isRightSelected"
            :badge-text="isRightSelected ? rightOptionText : ''"
            @select="handleSelect('right')"
          />
        </div>
      </div>
    </div>

    <!-- 하단 중앙 스티키 CTA -->
    <div class="fixed bottom-2 left-1/2 -translate-x-1/2 z-50" :style="sheetSafeAreaStyle">
      <div class="frosted-glass rounded-2xl shadow-xl border px-3 py-2">
        <Button
          size="lg"
          class="h-10 md:h-11 px-6 text-base bg-[hsl(var(--primary))] hover:bg-[hsl(var(--primary)/0.9)] text-[hsl(var(--primary-foreground))]"
          @click="$router.push('/matching')"
        >매칭 페이지로</Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { useTopicSetStore } from '@/store/topicSet'
import { useTopicSetController } from '@/composables/useTopicSetController'
import IconCell from '@/components/IconCell.vue'
import { ChevronLeft, ChevronRight, HelpCircle } from 'lucide-vue-next'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import debateLeft from '@/assets/images/profile/debate_left.png'
import debateRandom from '@/assets/images/profile/debate_random.png'
import debateRight from '@/assets/images/profile/debate_right.png'

// Store & 컨트롤러
const topicSetStore = useTopicSetStore()
const { remainingTimeSeconds } = useTopicSetController()

// 상태
const currentTopicIndex = ref(0)
const selectedAnimalIndex = ref<number | null>(null) // 0: left(북극곰), 2: right(펭귄)
const snapHighlightSide = ref<'left' | 'right' | null>(null)

// 리소스
const animals = [
  { name: '북극곰', avatar: debateLeft },
  { name: '물범',  avatar: debateRandom },
  { name: '펭귄',  avatar: debateRight  },
]

// 계산된 속성
const currentTopic = computed(() => topicSetStore.currentSet?.topics?.[currentTopicIndex.value] || null)
const totalTopics = computed(() => topicSetStore.currentSet?.topics?.length || 0)
const isLeftSelected = computed(() => selectedAnimalIndex.value === 0)
const isRightSelected = computed(() => selectedAnimalIndex.value === 2)
// 선택된 동물명(현재 UI 표기는 하단 바에서 대체되어 미사용)

// 남은 시간 포맷 (MM:SS 또는 HH:MM:SS)
const formattedRemainingTime = computed(() => {
  const total = remainingTimeSeconds.value || 0
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  const pad = (n: number) => String(n).padStart(2, '0')
  return hours > 0 ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}` : `${pad(minutes)}:${pad(seconds)}`
})

const leftOptionText = computed(() => currentTopic.value?.option1 || '')
const rightOptionText = computed(() => currentTopic.value?.option2 || '')

const sheetSafeAreaStyle = computed(() => ({ paddingBottom: 'max(8px, env(safe-area-inset-bottom))' }))

// IconCell이 자동 보정을 수행하므로 래퍼 보정 로직 제거

// 이벤트
function handleSelect(side: 'left' | 'right') {
  selectedAnimalIndex.value = side === 'left' ? 0 : 2
}

function handleRandom() {
  const side: 'left' | 'right' = Math.random() < 0.5 ? 'left' : 'right'
  selectedAnimalIndex.value = side === 'left' ? 0 : 2
  snapHighlightSide.value = side
  setTimeout(() => { snapHighlightSide.value = null }, 200)
}

// 주제 전환(버튼/시간 둘 다)
function nextTopic() {
  if (totalTopics.value === 0) return
  currentTopicIndex.value = (currentTopicIndex.value + 1) % totalTopics.value
}
function prevTopic() {
  if (totalTopics.value === 0) return
  currentTopicIndex.value = (currentTopicIndex.value - 1 + totalTopics.value) % totalTopics.value
}

// 30초마다 다음 주제로 넘어가는 보조 타이머(스토어의 시간교체와 별개로 UX 보조)
const secondsForAutoNext = 30
const topicCountdown = ref(secondsForAutoNext)
// UI에서 카운트다운 텍스트는 숨김 처리(요청)
let countdownTimer: number | null = null

function startTopicCountdown() {
  stopTopicCountdown()
  topicCountdown.value = secondsForAutoNext
  countdownTimer = setInterval(() => {
    if (topicCountdown.value <= 1) {
      nextTopic()
      topicCountdown.value = secondsForAutoNext
    } else {
      topicCountdown.value -= 1
    }
  }, 1000) as unknown as number
}
function stopTopicCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// 생명주기
onMounted(async () => {
  // 주제 세트 로드는 useTopicSetController(싱글톤)에서 담당
  startTopicCountdown()
  // 최초 진입 시 왼쪽/오른쪽 중 하나를 랜덤 프리셀렉션
  if (selectedAnimalIndex.value === null) {
    selectedAnimalIndex.value = Math.random() < 0.5 ? 0 : 2
  }
})

onUnmounted(() => {
  stopTopicCountdown()
})

// 주제가 바뀔 때마다(버튼/자동/세트 교체 포함) 무작위로 선택되도록 보장
watch(
  () => currentTopic.value?.id,
  () => {
    selectedAnimalIndex.value = Math.random() < 0.5 ? 0 : 2
  }
)
</script>

<style scoped>
/* 옵션 텍스트 최대 2줄 클램프 */
.clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-clamp: 2;
}
.clamp-5 {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-clamp: 5;
}
/* 1안 전용 3줄 클램프 */
.clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-clamp: 3;
}

/* 아이콘 정렬 프레임: 모든 아이콘 동일 높이/정렬 */
  .icon-frame {
  width: clamp(110px, 22vw, 220px);
  height: clamp(110px, 22vw, 220px);
  display: flex;
  align-items: flex-end; /* 발을 기준으로 맞춤 */
  justify-content: center;
}
.badge-pop-enter-active, .badge-pop-leave-active { transition: all 160ms cubic-bezier(0.22, 1, 0.36, 1); }
.badge-pop-enter-from { transform: translateY(6px) scale(0.96); opacity: 0; }
.badge-pop-enter-to   { transform: translateY(0)   scale(1);    opacity: 1; }
.badge-pop-leave-from { transform: translateY(0)   scale(1);    opacity: 1; }
.badge-pop-leave-to   { transform: translateY(6px) scale(0.96); opacity: 0; }
.icon-img {
  width: 100%;
  height: auto;
  transition: transform 300ms ease-out;
}
</style>