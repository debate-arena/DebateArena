import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useTopicSetStore } from '@/store/topicSet'
import { TopicSetStatus } from '@/constants/topicSet'

/**
 * useTopicSetController - 토픽 세트 관리 및 자동 교체 (새로운 설계)
 * - 1초씩 감소하는 단순한 타이머
 * - 자동 세트 교체
 * - 에러 복구 및 탭 가시성 처리
 */
export function useTopicSetController() {
  const store = useTopicSetStore()
  
  let timer: ReturnType<typeof setInterval> | null = null
  let ticking = false

  // 현재 활성 주제들
  const activeTopics = computed(() => store.currentSet?.topics || [])

  // 1초마다 실행되는 타이머
  const startTimer = () => {
    if (timer) return // 이미 실행 중이면 중복 방지
    
    timer = setInterval(() => {
      if (ticking) return // 재진입 방지
      ticking = true
      
      try {
        if (store.status === TopicSetStatus.ERROR) {
          // 에러 상태면 타이머 중단
          stopTimer()
          ticking = false
          return
        }
        
        if (!store.currentSet) {
          ticking = false
          return
        }
        
        // 시간 감소
        store.decrementTime()
        
        // 시간이 0이 되면 주제 교체
        if (store.remainingTimeSeconds <= 0) {
          store.swapSets()
        }
        
      } finally {
        ticking = false
      }
    }, 1000)
  }

  // 타이머 정지
  const stopTimer = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  // 탭 복귀 시 타이머 재시작
  const handleVisibility = () => {
    if (document.visibilityState === 'visible') {
      if (!timer) {
        startTimer()
      }
    }
  }

  // 에러 복구 후 타이머 재가동 (READY 전환 감시)
  watch(
    () => store.status,
    (newStatus, oldStatus) => {
      if (oldStatus === TopicSetStatus.ERROR && newStatus === TopicSetStatus.READY) {
        startTimer()
      }
    }
  )

  onMounted(async () => {
    await store.fetchTopicSets()
    startTimer()
    window.addEventListener('visibilitychange', handleVisibility)
  })

  onUnmounted(() => {
    stopTimer()
    window.removeEventListener('visibilitychange', handleVisibility)
  })

  return {
    activeTopics,
    store,
    remainingTimeSeconds: computed(() => store.remainingTimeSeconds)
  }
} 