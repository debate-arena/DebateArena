import { ref, computed, readonly } from 'vue'
import type { Ref } from 'vue'
import { useMatchingStore } from '@/store/matching'

export interface MatchingState {
  isMatching: boolean
  status: 'idle' | 'waiting' | 'matched' | 'connecting' | 'error'
  elapsedTime: number
  error: string | null
}

export interface RoomInfo {
  roomId: string
  totalUsers: number
  connectedUsers: number
}

export function useMatchingState() {
  const matchingStore = useMatchingStore()
  
  // 매칭 상태
  const matchingState = ref<MatchingState>({
    isMatching: false,
    status: 'idle',
    elapsedTime: 0,
    error: null
  })

  // 방 정보
  const roomInfo = ref<RoomInfo>({
    roomId: '',
    totalUsers: 0,
    connectedUsers: 0
  })

  // 연결 상태
  const isConnecting = ref(false)

  // 현재 매칭 ID
  const currentMatchId = ref<string>('')

  // 수락 타이머 관련
  const acceptTimeLeft = ref(30)
  let acceptTimer: ReturnType<typeof setInterval> | null = null

  // 계산된 속성들
  const canStartMatching = computed(() => {
    return matchingStore.canStartMatching && matchingState.value.status === 'idle' && !matchingState.value.error
  })

  const isMatchingActive = computed(() => {
    const isActive = matchingState.value.isMatching && matchingState.value.status === 'waiting'
    console.log('🔍 isMatchingActive 계산:', {
      isMatching: matchingState.value.isMatching,
      status: matchingState.value.status,
      result: isActive
    })
    return isActive
  })

  const isMatched = computed(() => {
    return matchingState.value.status === 'matched'
  })

  const isConnectingActive = computed(() => {
    return matchingState.value.status === 'connecting' || isConnecting.value
  })

  // 상태 업데이트 함수들
  const startMatching = () => {
    matchingState.value = {
      isMatching: true,
      status: 'waiting',
      elapsedTime: 0,
      error: null
    }
  }

  const stopMatching = () => {
    matchingState.value = {
      isMatching: false,
      status: 'idle',
      elapsedTime: 0,
      error: null
    }
    stopAcceptTimer()
  }

  const setMatched = () => {
    matchingState.value.status = 'matched'
  }

  const setConnecting = () => {
    matchingState.value.status = 'connecting'
    isConnecting.value = true
  }

  const setError = (error: string) => {
    matchingState.value.error = error
    matchingState.value.status = 'error'
  }

  const clearError = () => {
    matchingState.value.error = null
  }

  const updateElapsedTime = (time: number) => {
    matchingState.value.elapsedTime = time
  }

  // 방 정보 업데이트
  const updateRoomInfo = (info: Partial<RoomInfo>) => {
    Object.assign(roomInfo.value, info)
  }

  const resetRoomInfo = () => {
    roomInfo.value = {
      roomId: '',
      totalUsers: 0,
      connectedUsers: 0
    }
  }

  // 수락 타이머 관리
  const startAcceptTimer = () => {
    if (acceptTimer) {
      clearInterval(acceptTimer)
    }
    
    acceptTimeLeft.value = 10
    acceptTimer = setInterval(() => {
      acceptTimeLeft.value -= 0.2
      if (acceptTimeLeft.value <= 0) {
        stopAcceptTimer()
        setError('수락 시간이 만료되었습니다.')
        console.log('⏰ 수락 타이머 만료됨')
      }
    }, 200)
  }

  const stopAcceptTimer = () => {
    if (acceptTimer) {
      clearInterval(acceptTimer)
      acceptTimer = null
    }
  }

  const resetAcceptTimer = () => {
    acceptTimeLeft.value = 10
  }

  // 전체 초기화
  const reset = () => {
    // 매칭 상태 초기화
    matchingState.value = {
      isMatching: false,
      status: 'idle',
      elapsedTime: 0,
      error: null
    }
    
    // 방 정보 초기화
    resetRoomInfo()
    
    // 타이머 정리
    stopAcceptTimer()
    resetAcceptTimer()
    
    // 기타 상태 초기화
    currentMatchId.value = ''
    isConnecting.value = false
    
    console.log('✅ 매칭 상태 초기화 완료')
  }

  return {
    // 상태
    matchingState: readonly(matchingState),
    roomInfo: readonly(roomInfo),
    isConnecting: readonly(isConnecting),
    currentMatchId,
    acceptTimeLeft,

    // 계산된 속성들
    canStartMatching,
    isMatchingActive,
    isMatched,
    isConnectingActive,

    // 상태 업데이트 함수들
    startMatching,
    stopMatching,
    setMatched,
    setConnecting,
    setError,
    clearError,
    updateElapsedTime,
    updateRoomInfo,
    resetRoomInfo,

    // 타이머 관리
    startAcceptTimer,
    stopAcceptTimer,
    resetAcceptTimer,

    // 전체 초기화
    reset
  }
} 