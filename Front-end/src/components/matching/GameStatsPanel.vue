<template>
  <Card class="min-h-[400px] border-0">
    <CardContent class="p-8">
      <!-- 중앙 매칭 이미지 -->
      <div class="flex flex-col items-center justify-center space-y-6 text-foreground">
        <!-- 매칭 이미지 -->
        <div class="w-32 h-32 flex items-center justify-center relative">
          <img 
            src="@/assets/images/profile/waiting.png" 
            alt="매칭 중" 
            class="w-full h-full object-contain animate-pulse polar-icon"
          />
          <!-- 매칭 중 애니메이션 효과 -->
          <div class="absolute inset-0 rounded-full border-2 border-blue-400/30 animate-ping"></div>
        </div>
        
        <!-- 매칭 텍스트 -->
        <div class="text-center">
          <p class="text-sm text-slate-400 mt-1">물고기 기다리는 중...</p>
          <!-- 점 애니메이션 -->
          <div class="flex justify-center mt-2 space-x-1">
            <div class="w-2 h-2 bg-blue-400 rounded-full animate-bounce"></div>
            <div class="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 0.1s"></div>
            <div class="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
          </div>
        </div>
        
        <!-- 매칭 타이머 -->
          <div class="text-center mt-6">
            <div class="text-4xl font-bold">{{ formatTime(matchingTime) }}</div>
        </div>
        
        <!-- 선택한 주제 카드들 -->
        <div class="w-full mt-8">
          <div class="flex items-center justify-center gap-2 mb-4">
            <h3 class="text-sm font-medium">선택한 주제</h3>
            <div class="relative group">
              <HelpCircle class="w-4 h-4 text-muted-foreground hover:text-foreground cursor-help" />
              <!-- 커스텀 툴팁 -->
              <div class="absolute left-0 bottom-full w-80 p-4 bg-white dark:bg-slate-800 border border-border rounded-md shadow-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50">
                <div class="space-y-3">
                  <h4 class="font-medium text-foreground">게임 규칙</h4>
                  <div class="text-xs space-y-2 text-muted-foreground">
                    <div class="space-y-1">
                      <p class="font-medium">🎯 게임 진행</p>
                      <p>준비 30초 → 입장 1분 → 공격 대상 선택 30초 → 공격 30초 → 방어 30초 → 최종 투표 30초 → AI 판정</p>
                    </div>
                    
                    <div class="space-y-1">
                      <p class="font-medium">⚠️ 주의사항</p>
                      <p>1. 마이크를 허용해야 플레이가 가능해요</p>
                      <p>2. <img src="@/assets/images/profile/debate_random.png" class="inline w-4 h-4 polar-icon" alt="물범" /> 선택시 <img src="@/assets/images/profile/debate_left.png" class="inline w-4 h-4 polar-icon" alt="북극곰" />, <img src="@/assets/images/profile/debate_right.png" class="inline w-4 h-4 polar-icon" alt="펭귄" />가 랜덤으로 선택되요</p>
                      <p>3. 발언 순서는 <img src="@/assets/images/profile/debate_left.png" class="inline w-4 h-4 polar-icon" alt="북극곰" /> → <img src="@/assets/images/profile/debate_right.png" class="inline w-4 h-4 polar-icon" alt="펭귄" /> 순서로 반복</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="flex gap-4 justify-center overflow-x-auto pb-2">
            <!-- 주제 카드들 -->
            <Card 
              v-for="topic in selectedTopics" 
              :key="topic.id"
              class="flex-shrink-0 w-48 p-3"
            >
              <div class="space-y-2">
                                       <!-- 주제 제목 (항상 두 줄) -->
                       <div class="font-medium text-foreground text-center h-10 flex items-center justify-center leading-tight">
                         <span class="break-words whitespace-pre-line">{{ formatTitleToTwoLines(topic.title) }}</span>
                       </div>
                
                <!-- 선택한 진영 -->
                <div class="flex justify-center items-center gap-2">
                  <Badge 
                    :class="getStanceBadgeClass(topic.stance)"
                    class="text-xs flex items-center gap-1"
                  >
                    <!-- 진영 아이콘 (선택되었을 때만 표시) -->
                    <img 
                      v-if="topic.stance === 'option1'"
                      src="@/assets/images/profile/debate_left.png" 
                      class="w-3 h-3 polar-icon" 
                      alt="북극곰" 
                    />
                    <img 
                      v-else-if="topic.stance === 'option2'"
                      src="@/assets/images/profile/debate_right.png" 
                      class="w-3 h-3 polar-icon" 
                      alt="펭귄" 
                    />
                    <img 
                      v-else-if="topic.stance === 'random'"
                      src="@/assets/images/profile/debate_random.png" 
                      class="w-3 h-3 polar-icon" 
                      alt="물범" 
                    />
                    {{ getStanceText(topic.stance, matchingStore.getTopicById(topic.id)) }}
                  </Badge>
                </div>
                
                <!-- 선택한 모드 -->
                <div class="flex gap-1 justify-center">
                  <Badge 
                    v-for="mode in topic.modes" 
                    :key="mode"
                    :class="getModeBadgeClass(mode)"
                    class="text-xs"
                  >
                    {{ mode }}
                  </Badge>
                </div>
              </div>
            </Card>
            
            <!-- 주제가 없을 때 -->
            <div v-if="selectedTopics.length === 0" class="text-center text-muted-foreground">
              <p class="text-sm">선택된 주제가 없습니다</p>
            </div>
          </div>
        </div>
      </div>
    </CardContent>
  </Card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { useMatchingStore } from '@/store/matching'
import { HelpCircle } from 'lucide-vue-next'
import type { PlayerMode } from '@/types/matching'
import { getStanceText, getStanceBadgeClass, getModeBadgeClass } from '@/utils/matching'

// 매칭 스토어 연동
const matchingStore = useMatchingStore()

// 매칭 타이머 (스토어에서 가져옴)
const matchingTime = computed(() => matchingStore.elapsedTime)

// 시간 포맷팅 함수 (mm:ss)
const formatTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 제목을 강제로 두 줄로 만드는 함수
const formatTitleToTwoLines = (title: string) => {
  // 줄바꿈이 없으면 끝에 빈 줄 추가
  if (!title.includes('\n')) {
    return title + '\n'
  }
  return title
}

// 실제 선택된 주제들 가져오기
const selectedTopics = computed(() => {
  return matchingStore.selectedTopicSelections.map(selection => {
    const topic = matchingStore.getTopicById(selection.topicId)
    return {
      id: selection.topicId,
      title: topic?.title || '알 수 없는 주제',
      stance: selection.stance,
      modes: selection.modes as PlayerMode[]
    }
  })
})


</script>
