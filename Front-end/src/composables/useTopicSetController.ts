import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useTopicSetStore } from '@/store/topicSet'
import { TopicSetStatus } from '@/constants/topicSet'

/**
 * useTopicSetController - 토픽 세트 관리 및 자동 교체 (새로운 설계)
 * - 1초씩 감소하는 단순한 타이머
 * - 자동 세트 교체
 * - 에러 복구 및 탭 가시성 처리
 */
// 싱글톤 타이머 상태 (모듈 전역)
let singletonTimer: ReturnType<typeof setInterval> | null = null
let isTicking = false
let subscriberCount = 0
let visibilityListenerAttached = false

export function useTopicSetController() {
  const store = useTopicSetStore()

  // 현재 활성 주제들
  const activeTopics = computed(() => store.currentSet?.topics || [])

  // 1초마다 실행되는 타이머
  const startTimer = () => {
    if (singletonTimer) return // 이미 실행 중이면 중복 방지

    singletonTimer = setInterval(() => {
      if (isTicking) return // 재진입 방지
      isTicking = true

      try {
        // Pinia 스토어는 싱글톤이므로 동일 인스턴스 사용
        const s = store

        if (s.status === TopicSetStatus.ERROR) {
          // 에러 상태면 타이머 중단
          stopTimer()
          isTicking = false
          return
        }

        if (!s.currentSet) {
          isTicking = false
          return
        }

        // 시간 감소 (1초)
        s.decrementTime()

        // 시간이 0이 되면 주제 교체
        if (s.remainingTimeSeconds <= 0) {
          s.swapSets()
        }

      } finally {
        isTicking = false
      }
    }, 1000)
  }

  // 타이머 정지
  const stopTimer = () => {
    if (singletonTimer) {
      clearInterval(singletonTimer)
      singletonTimer = null
    }
  }

  // 탭 복귀 시 타이머 재시작
  const handleVisibility = () => {
    if (document.visibilityState === 'visible') {
      if (!singletonTimer) {
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
    subscriberCount += 1
    // 최초 마운트 시에만 서버에서 세트 로드 (이미 있으면 재요청 생략)
    if (!store.currentSet || store.status === TopicSetStatus.INIT || store.isError) {
      await store.fetchTopicSets()
    }
    startTimer()
    if (!visibilityListenerAttached) {
      window.addEventListener('visibilitychange', handleVisibility)
      visibilityListenerAttached = true
    }
  })

  onUnmounted(() => {
    subscriberCount = Math.max(0, subscriberCount - 1)
    if (subscriberCount === 0) {
      stopTimer()
      if (visibilityListenerAttached) {
        window.removeEventListener('visibilitychange', handleVisibility)
        visibilityListenerAttached = false
      }
    }
  })

  return {
    activeTopics,
    store,
    remainingTimeSeconds: computed(() => store.remainingTimeSeconds)
  }
} 