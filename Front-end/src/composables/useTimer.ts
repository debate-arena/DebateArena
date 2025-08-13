import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useTopicSetStore } from '@/store/topicSet'
import { getRemainingMs } from '@/utils/topicSet'

/**
 * 공통 타이머 로직
 * - 1초마다 현재 시간 업데이트
 * - 남은 시간 실시간 계산
 * - 메모리 정리 자동화
 */
export function useTimer() {
  const topicSetStore = useTopicSetStore()
  
  // 1초마다 갱신되는 now ref
  const now = ref(Date.now())
  let timer: ReturnType<typeof setInterval> | null = null

  // 타이머 시작
  const startTimer = () => {
    if (timer) return // 이미 실행 중이면 무시
    timer = setInterval(() => {
      now.value = Date.now()
    }, 1000)
  }

  // 타이머 정리
  const stopTimer = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  // 남은 시간 계산 (실시간 업데이트)
  const remainingTime = computed(() => {
    if (!topicSetStore.currentSet) return 0
    
    const serverNowMs = now.value + topicSetStore.serverTimeOffsetMs
    const currentSetEnd = topicSetStore.currentSet.endAtMs
    const nextSetEnd = topicSetStore.nextSet?.endAtMs || currentSetEnd
    
    // 현재 시간이 어떤 세트에 해당하는지 판단
    let targetEndMs = currentSetEnd
    
    // 현재 시간이 첫 번째 세트 시간을 넘었으면 두 번째 세트 사용
    if (serverNowMs > currentSetEnd && topicSetStore.nextSet) {
      targetEndMs = nextSetEnd
    }
    
    const result = getRemainingMs(targetEndMs, serverNowMs)
    
    return Math.floor(result / 1000) // 밀리초를 초로 변환
  })

  // 시간 포맷팅 (MM:SS)
  const formatTime = (seconds: number) => {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
  }

  // 밀리초 포맷팅 (MM:SS)
  const formatMs = (ms: number) => {
    const totalSec = Math.floor(ms / 1000)
    const min = Math.floor(totalSec / 60)
    const sec = totalSec % 60
    return `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
  }

  onMounted(() => {
    startTimer()
  })

  onUnmounted(() => {
    stopTimer()
  })

  return {
    now,
    remainingTime,
    formatTime,
    formatMs,
    startTimer,
    stopTimer
  }
} 