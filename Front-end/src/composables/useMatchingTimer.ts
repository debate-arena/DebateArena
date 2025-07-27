import { ref, computed, onUnmounted } from 'vue'
import { useTopicSetStore } from '@/store/topicSet'
import { useMatchingStore } from '@/store/matching'

export function useMatchingTimer() {
  const topicSetStore = useTopicSetStore()
  const matchingStore = useMatchingStore()
  
  const now = ref(Date.now())
  let timerInterval: ReturnType<typeof setInterval> | null = null

  // 남은 시간 계산
  const remainingTime = computed(() => {
    if (!topicSetStore.currentSet) return 0
    
    const currentTime = now.value
    const endTime = topicSetStore.currentSet.endAtMs
    return Math.max(0, endTime - currentTime)
  })

  // 타이머 포맷팅
  const formatTime = (seconds: number) => {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
  }

  // 타이머 시작
  const startMatchingTimer = (onTimeout?: () => void) => {
    if (timerInterval) {
      clearInterval(timerInterval)
    }
    
    timerInterval = setInterval(() => {
      now.value = Date.now()
      
      // 타임아웃 체크
      if (checkTimeout()) {
        if (onTimeout) {
          onTimeout()
        }
        return
      }
      
      // 정각 체크
      if (checkHourlyTimeout()) {
        if (onTimeout) {
          onTimeout()
        }
        return
      }
      
      // 매칭 타이머 업데이트
      if (matchingStore.isMatching) {
        matchingStore.updateTimer()
      }
    }, 1000)
  }

  // 타이머 정지
  const stopMatchingTimer = () => {
    if (timerInterval) {
      clearInterval(timerInterval)
      timerInterval = null
    }
  }

  // 타임아웃 체크 (10분)
  const checkTimeout = () => {
    if (!matchingStore.isMatching) return false
    
    if (matchingStore.elapsedTime >= 600) { // 10분 = 600초
      console.log('⏰ 매칭 타임아웃 (10분 초과)')
      return true
    }
    return false
  }

  // 정각 체크 (정각 + 10초 후 자동 취소)
  const checkHourlyTimeout = () => {
    if (!matchingStore.isMatching) return false

    const minutesUntilChange = Math.floor(remainingTime.value / 60000) // 분 단위

    // 정각 + 10초 후 자동 취소
    if (remainingTime.value <= -10000) { // -10초 (정각 + 10초)
      console.log('⏰ 정각 + 10초 초과로 자동 취소')
      return true
    }
    return false
  }

  // 정각 5분 전 경고 표시 여부
  const showHourWarning = () => {
    return remainingTime.value <= 300000 // 5분 = 300초 = 300000ms
  }

  // 컴포넌트 언마운트 시 정리
  onUnmounted(() => {
    stopMatchingTimer()
  })

  return {
    remainingTime,
    formatTime,
    startMatchingTimer,
    stopMatchingTimer,
    checkTimeout,
    checkHourlyTimeout,
    showHourWarning
  }
} 