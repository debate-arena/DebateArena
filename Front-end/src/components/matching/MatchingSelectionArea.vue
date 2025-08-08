<template>
  <div class="space-y-4">
    <!-- 상단: 인원선택 + 타이머 + 매칭시작 통합 카드 (Sticky) -->
    <div class="sticky top-2 z-20 mb-4">
      <div class="relative mx-auto max-w-3xl px-6">
        <!-- 메인 헤더 카드 -->
        <div>
          <Card class="relative overflow-hidden border-0 bg-gradient-to-r from-background via-card to-background dark:from-background dark:via-card dark:to-background shadow-lg shadow-slate-200/20 dark:shadow-slate-950/50">
            <!-- 배경 데코레이션 -->
            <div class="absolute inset-0"></div>
            
            <!-- 상단 테두리 그라데이션 -->
            <div class="absolute top-0 left-0 right-0 h-px bg-border"></div>
            
            <!-- 메인 콘텐츠 -->
            <div class="relative px-6 py-4">
              <div class="flex items-center justify-between gap-6">
                <!-- 좌측: 인원 선택 -->
                <div class="flex flex-col items-center gap-1">
                  <div class="flex items-center gap-2">
                    <div class="w-2 h-2 rounded-full bg-slate-500 dark:bg-slate-400 animate-pulse"></div>
                    <span class="text-xs font-medium text-muted-foreground text-center">모드</span>
                  </div>
                  <div class="transform">
                    <PlayerCountSelection />
                  </div>
                </div>
                
                <!-- 중앙: 주제 변경 타이머 -->
                <div class="flex flex-col items-center">
                  <div class="h-5"></div>
                  <div class="flex items-center gap-3 px-5 h-12 rounded-full bg-card/90 dark:bg-card/90 backdrop-blur border border-border/50 shadow-sm">
                    <div class="flex items-center gap-2">
                      <Hourglass class="w-5 h-5 text-foreground" aria-hidden="true" />
                      <span class="text-sm text-muted-foreground">새 주제까지</span>
                    </div>
                    <div class="text-base font-mono font-semibold text-foreground">
                      {{ formattedTime }}
                    </div>
                  </div>
                </div>
                
                <!-- 우측: 매칭 시작 버튼 -->
                <div class="relative flex flex-col items-center">
                  <div class="h-5"></div>
                  <Button 
                    @click="$emit('start-matching')"
                    :disabled="!matchingStore.canStartMatching || matchingStore.isMatching || props.isStartingMatch"
                    class="relative min-w-[130px] h-12 text-base px-5 bg-primary hover:bg-primary/90 text-primary-foreground font-medium shadow-lg hover:shadow-xl transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <!-- 버튼 내부 미묘한 하이라이트 -->
                    <div class="absolute inset-0 bg-gradient-to-r from-white/10 to-transparent rounded-md"></div>
                    
                    <span v-if="!matchingStore.isMatching" class="relative flex items-center gap-2">
                      <Play class="w-5 h-5" aria-hidden="true" />
                      게임 시작
                    </span>
                    <span v-else-if="matchingStore.status === 'matched' || matchingStore.status === 'connecting'" class="relative flex items-center gap-2">
                      <CheckCircle2 class="w-5 h-5" aria-hidden="true" />
                      매칭 성사
                    </span>
                    <span v-else class="relative flex items-center gap-2">
                      <div class="w-5 h-5 border-2 border-white dark:border-black border-t-transparent rounded-full animate-spin"></div>
                      {{ props.isStartingMatch ? '시작 중...' : '매칭 중...' }}
                    </span>
                  </Button>
                  
                  <!-- 취소 버튼 -->
                  <Button
                    v-if="matchingStore.isMatching && matchingStore.status !== 'matched' && matchingStore.status !== 'connecting'"
                    @click="$emit('cancel-matching')"
                    variant="destructive"
                    size="sm"
                    class="absolute -top-2 -right-2 h-6 w-6 rounded-full p-0 shadow-lg hover:shadow-xl transition-all duration-200"
                  >
                    <span class="text-xs font-bold">×</span>
                  </Button>
                </div>
              </div>
              
              <!-- 조건 안내 -->
              <div v-if="!matchingStore.canStartMatching" class="mt-3 pt-3 border-t border-border/50">
                <div class="flex items-center justify-center gap-2 text-xs text-muted-foreground">
                  <span class="text-base">⚠️</span>
                  <span>주제와 모드를 각각 최소 1개씩 선택해주세요</span>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>

    <!-- 주제 카드들 (상단 글로벌 영역과 동일 폭/여백 정렬) -->
    <div class="space-y-4 mx-auto max-w-3xl px-6">
      <!-- 토픽 로딩 중 -->
      <div v-if="topicSetStore.isLoading" class="text-center py-8">
        <div class="animate-pulse text-slate-600">주제를 불러오는 중...</div>
      </div>
      
      <!-- 토픽 에러 -->
      <div v-else-if="topicSetStore.isError" class="text-center py-8 text-red-500">
        <div>주제를 불러오지 못했습니다</div>
        <button @click="topicSetStore.fetchTopicSets()" class="mt-2 text-sm underline">
          다시 시도
        </button>
      </div>
      
      <!-- 실제 토픽 카드들 -->
      <TopicCard
        v-for="topic in activeTopics"
        :key="topic.id"
        :topic="topic"
      />
      
      <!-- 토픽이 없는 경우 -->
      <div v-if="!topicSetStore.isLoading && !topicSetStore.isError && activeTopics.length === 0" class="text-center py-8 text-slate-600">
        토픽을 불러오는 중입니다...
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { useMatchingStore } from '@/store/matching'
import { useTopicSetStore } from '@/store/topicSet'
import { useTopicSetController } from '@/composables/useTopicSetController'
import PlayerCountSelection from '@/components/matching/PlayerCountSelection.vue'
import TopicCard from '@/components/matching/TopicCard.vue'
import { formatTimer } from '@/utils/matching'
import { Hourglass, Play, CheckCircle2 } from 'lucide-vue-next'

// Props
interface Props {
  isStartingMatch?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isStartingMatch: false
})

// 스토어 및 컴포저블
const matchingStore = useMatchingStore()
const topicSetStore = useTopicSetStore()
const { activeTopics, remainingTimeSeconds } = useTopicSetController()

// 이벤트 emit 정의
defineEmits<{
  'start-matching': []
  'cancel-matching': []
}>()

// 남은 시간 포맷팅
const formattedTime = computed(() => {
  return formatTimer(remainingTimeSeconds.value)
})

// 활성 토픽이 변경될 때 매칭 선택 상태 초기화
watch(activeTopics, (newTopics) => {
  if (newTopics.length > 0) {
    const topicIds = newTopics.map(topic => topic.id)
    matchingStore.initializeTopicSelections(topicIds)
  }
}, { immediate: true })
</script>
