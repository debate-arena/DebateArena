<template>
  <div class="p-6 space-y-6">
    <!-- 제목 -->
    <div class="text-center">
      <h1 class="text-2xl font-bold">🎤 음성 인식 데모</h1>
      <p class="text-muted-foreground">Web Speech API를 사용한 실시간 음성 인식</p>
    </div>

    <!-- 상태 표시 -->
    <div class="flex items-center justify-center gap-4">
      <div class="flex items-center gap-2">
        <div 
          class="w-3 h-3 rounded-full" 
          :class="running ? 'bg-green-500 animate-pulse' : 'bg-gray-400'"
        ></div>
        <span class="text-sm">{{ running ? '인식 중...' : '대기 중' }}</span>
      </div>
      
      <div class="text-sm text-muted-foreground">
        {{ counts.chars }}자 / {{ counts.words }}단어
      </div>
    </div>

    <!-- 컨트롤 버튼 -->
    <div class="flex justify-center gap-4">
      <Button 
        @click="start" 
        :disabled="running"
        class="bg-green-600 hover:bg-green-700"
      >
        🎤 시작
      </Button>
      
      <Button 
        @click="stop" 
        :disabled="!running"
        variant="destructive"
      >
        ⏹️ 정지
      </Button>
      
      <Button 
        @click="flush" 
        :disabled="!running || !preview"
        variant="outline"
      >
        ✂️ 수동 분할
      </Button>
    </div>

    <!-- 실시간 미리보기 -->
    <Card v-if="preview || running">
      <CardHeader>
        <CardTitle class="text-lg">실시간 미리보기</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="min-h-[100px] p-4 bg-muted rounded-lg">
          <p v-if="preview" class="text-foreground">{{ preview }}</p>
          <p v-else class="text-muted-foreground italic">
            음성을 말씀해주세요...
          </p>
        </div>
      </CardContent>
    </Card>

    <!-- 완성된 세그먼트들 -->
    <Card v-if="segments.length > 0">
      <CardHeader>
        <CardTitle class="text-lg">완성된 세그먼트 ({{ segments.length }}개)</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="space-y-3">
          <div 
            v-for="segment in segments" 
            :key="segment.id"
            class="p-4 border rounded-lg"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1">
                <p class="text-foreground">{{ segment.text }}</p>
                <div class="flex items-center gap-2 mt-2">
                  <Badge :variant="getReasonVariant(segment.reason)">
                    {{ getReasonText(segment.reason) }}
                  </Badge>
                  <span class="text-xs text-muted-foreground">
                    {{ formatTime(segment.at) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 에러 메시지 -->
    <Alert v-if="lastError" variant="destructive">
      <AlertCircle class="h-4 w-4" />
      <AlertTitle>오류</AlertTitle>
      <AlertDescription>{{ lastError }}</AlertDescription>
    </Alert>

    <!-- 설정 -->
    <Card>
      <CardHeader>
        <CardTitle class="text-lg">설정</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div>
            <span class="font-medium">최대 글자수:</span> {{ config.maxChars }}
          </div>
          <div>
            <span class="font-medium">최대 단어수:</span> {{ config.maxWords }}
          </div>
          <div>
            <span class="font-medium">침묵 타임아웃:</span> {{ config.silenceMs }}ms
          </div>
          <div>
            <span class="font-medium">지연 타임아웃:</span> {{ config.maxLatencyMs }}ms
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { AlertCircle } from 'lucide-vue-next'
import { useSpeechRecognition, type FlushReason } from '@/composables/useSpeechRecognition'

// 음성 인식 컴포저블 사용
const {
  running,
  preview,
  segments,
  counts,
  lastError,
  start,
  stop,
  flush,
  config
} = useSpeechRecognition({
  maxChars: 200,        // 더 짧게 설정
  maxWords: 30,         // 더 짧게 설정
  silenceMs: 1000,      // 1초 침묵
  maxLatencyMs: 3000,   // 3초 지연
  autoStopMs: 30000,    // 30초 자동 종료
})

// 유틸리티 함수들
const getReasonText = (reason: FlushReason) => {
  const reasons = {
    punct: '문장부호',
    size: '크기 제한',
    silence: '침묵',
    latency: '지연',
    end: '종료',
    error: '오류',
    manual: '수동'
  }
  return reasons[reason] || reason
}

const getReasonVariant = (reason: FlushReason) => {
  const variants = {
    punct: 'default',
    size: 'secondary',
    silence: 'outline',
    latency: 'outline',
    end: 'destructive',
    error: 'destructive',
    manual: 'default'
  }
  return variants[reason] || 'default'
}

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('ko-KR', { 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit' 
  })
}
</script> 