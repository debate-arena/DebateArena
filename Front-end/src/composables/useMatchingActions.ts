import { useMatchingStore } from '@/store/matching'
import { useWebSocket } from '@/composables/useWebSocket'
import { useMatchingModals } from '@/composables/useMatchingModals'
import { useMatchingTimer } from '@/composables/useMatchingTimer'
import { useAuthStore } from '@/store/auth'
import type { MatchModalData } from '@/composables/useMatchingModals'

export function useMatchingActions() {
  const matchingStore = useMatchingStore()
  const webSocket = useWebSocket()
  const modals = useMatchingModals()
  const authStore = useAuthStore()
  const { startMatchingTimer, stopMatchingTimer, startAcceptTimer, stopAcceptTimer } = useMatchingTimer()

  // 매칭 시작
  const startMatching = async () => {
    try {
      // WebSocket 연결
      await webSocket.connect()
      
      // 매칭 요청 전송
      const request = matchingStore.toMatchRequest
      webSocket.sendMatchRequest(request)
      
      // 상태 업데이트 (matchingStore만 사용)
      matchingStore.startMatching()
      
      // 타이머 시작
      startMatchingTimer(() => {
        handleTimeout()
      })
      
      console.log('✅ 매칭 시작 완료')
    } catch (error) {
      console.error('❌ 매칭 시작 실패:', error)
      matchingStore.setError('매칭 시작에 실패했습니다.')
    }
  }

  // 매칭 취소
  const cancelMatching = () => {
    console.log('❌ 매칭 취소 요청')
    
    // WebSocket 연결 해제
    webSocket.disconnect()
    
    // 타이머 정지
    stopMatchingTimer()
    
    // 상태 초기화 (matchingStore만 사용)
    matchingStore.cancelMatching()
    
    // 모든 모달 닫기
    modals.hideAllModals()
    
    console.log('✅ 매칭 취소 완료')
  }

  // 매칭 수락
  const acceptMatch = () => {
    console.log('✅ 매칭 수락')
    
    // 연결 상태로 변경
    matchingStore.setConnecting()
    
    // 서버에 수락 메시지 전송
    const matchId = matchingStore.currentMatchId
    if (matchId) {
      // 실제 stance를 숫자로 변환해서 전송
      const actualStance = 'option1'  // 실제 선택한 진영
      const stanceNumber = actualStance === 'option1' ? 0 : 1  // 0: option1, 1: option2
      webSocket.sendMatchAcceptance(matchId, true, stanceNumber)
    }
  }

  // 매칭 거절
  const rejectMatch = () => {
    console.log('❌ 매칭 거절')
    
    // 서버에 거절 메시지 전송
    const matchId = matchingStore.currentMatchId
    if (matchId) {
      // 실제 stance를 숫자로 변환해서 전송
      const actualStance = 'option1'  // 실제 선택한 진영
      const stanceNumber = actualStance === 'option1' ? 0 : 1  // 0: option1, 1: option2
      webSocket.sendMatchAcceptance(matchId, false, stanceNumber)
    }
    
    // 모달 닫기
    modals.hideMatchCompleteModal()
  }

  // 매칭 성사 처리
  const handleMatchSuccess = (data: any) => {
    console.log('🎉 매칭 성사 처리 시작:', data)
    console.log('🔍 입력 데이터 타입:', typeof data)
    console.log('🔍 입력 데이터 키들:', Object.keys(data))
    
    // 매칭 ID 저장
    const matchId = data.matchId || 'default-match-id'
    matchingStore.setCurrentMatchId(matchId)
    console.log('🔍 매칭 ID 저장됨:', matchId)
    
    // 매칭 상태로 변경
    matchingStore.setMatched()
    console.log('🔍 매칭 상태 변경됨:', matchingStore.status)
    
    // 연결된 사용자 수 초기화 (0으로 시작)
    matchingStore.updateRoomInfo({ connectedUsers: 0 })
    console.log('🔍 연결된 사용자 수 초기화: 0')
    
    // 모달 데이터 준비
    const modalData: MatchModalData = {
      topicTitle: data.topicTitle || '매칭된 주제',
      stanceText: data.stanceText || '선택된 진영',
      mode: data.mode || '1:1',
      topicId: data.topicId || 1
    }
    console.log('🔍 모달 데이터 준비됨:', modalData)
    
    // 매칭 성사 상태로 설정 (isMatching은 true 유지)
    matchingStore.isMatching = true
    matchingStore.status = 'matched'
    console.log('🔍 매칭 성사 상태로 설정됨')
    
    // 모달 표시 (사용자 데이터 없이)
    console.log('🔍 모달 표시 시도...')
    modals.showMatchCompleteModal(modalData)
    console.log('🔍 모달 표시 완료, 현재 모달 상태:', modals.modalState)
    
    // 모달이 표시되자마자 타이머 시작 (즉시)
    console.log('🔍 수락 타이머 시작 (즉시)')
    console.log('🔍 타이머 시작 전 상태:', {
      matchingStoreStatus: matchingStore.status,
      acceptTimeLeft: matchingStore.acceptTimeLeft
    })
    
    // 타이머 시작 순서 중요: 먼저 store에서 시작
    matchingStore.startAcceptTimer()
    console.log('🔍 store 타이머 시작됨')
    
    // 그 다음 composable에서 시작 (이때 isAcceptTimerActive가 true로 설정됨)
    startAcceptTimer() // 타이머 composable에서도 시작
    console.log('🔍 composable 타이머 시작됨')
    console.log('🔍 수락 타이머 시작됨:', matchingStore.acceptTimeLeft)
    
    console.log('✅ 매칭 성사 처리 완료')
  }

  // 타임아웃 처리
  const handleTimeout = () => {
    console.log('⏰ 매칭 타임아웃')
    
    // 매칭 중지
    matchingStore.cancelMatching()
    
    // 타임아웃 모달 표시
    modals.showTimeoutModal()
    
    console.log('✅ 타임아웃 처리 완료')
  }

  // 에러 처리
  const handleError = (error: string) => {
    console.error('❌ 매칭 에러:', error)
    
    // 에러 상태 설정
    matchingStore.setError(error)
    
    // 매칭 중지
    matchingStore.cancelMatching()
    
    console.log('✅ 에러 처리 완료')
  }

  return {
    startMatching,
    cancelMatching,
    acceptMatch,
    rejectMatch,
    handleMatchSuccess,
    handleTimeout,
    handleError
  }
} 