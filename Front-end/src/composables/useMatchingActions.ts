import { useMatchingStore } from '@/store/matching'
import { useWebSocket } from '@/composables/useWebSocket'
import { useMatchingState } from '@/composables/useMatchingState'
import { useMatchingModals } from '@/composables/useMatchingModals'
import { useMatchingTimer } from '@/composables/useMatchingTimer'
import type { MatchModalData, MatchUser } from '@/composables/useMatchingModals'

export function useMatchingActions() {
  const matchingStore = useMatchingStore()
  const webSocket = useWebSocket()
  const matchingState = useMatchingState()
  const modals = useMatchingModals()
  const { startMatchingTimer, stopMatchingTimer } = useMatchingTimer()

  // 매칭 시작
  const startMatching = async () => {
    try {
      // WebSocket 연결
      await webSocket.connect()
      
      // 매칭 요청 전송
      const request = matchingStore.toMatchRequest
      webSocket.sendMatchRequest(request)
      
      // 상태 업데이트 (matchingState와 matchingStore 모두)
      matchingState.startMatching()
      matchingStore.startMatching()
      
      // 타이머 시작
      startMatchingTimer(() => {
        handleTimeout()
      })
      
      console.log('✅ 매칭 시작 완료')
    } catch (error) {
      console.error('❌ 매칭 시작 실패:', error)
      matchingState.setError('매칭 시작에 실패했습니다.')
    }
  }

  // 매칭 취소
  const cancelMatching = () => {
    console.log('❌ 매칭 취소 요청')
    
    // WebSocket 연결 해제
    webSocket.disconnect()
    
    // 타이머 정지
    stopMatchingTimer()
    
    // 상태 초기화 (matchingState와 matchingStore 모두)
    matchingState.reset()
    matchingStore.cancelMatching()
    
    // 모든 모달 닫기
    modals.hideAllModals()
    
    console.log('✅ 매칭 취소 완료')
  }

  // 매칭 수락
  const acceptMatch = () => {
    console.log('✅ 매칭 수락')
    
    // 연결 상태로 변경
    matchingState.setConnecting()
    
    // 서버에 수락 메시지 전송
    const matchId = matchingState.currentMatchId.value
    if (matchId) {
      webSocket.sendMatchAcceptance(matchId, true)
    }
    
    // 수락 타이머 시작
    matchingState.startAcceptTimer()
  }

  // 매칭 거절
  const rejectMatch = () => {
    console.log('❌ 매칭 거절')
    
    // 서버에 거절 메시지 전송
    const matchId = matchingState.currentMatchId.value
    if (matchId) {
      webSocket.sendMatchAcceptance(matchId, false)
    }
    
    // 모달 닫기
    modals.hideMatchCompleteModal()
    
    // 타이머 정지
    matchingState.stopAcceptTimer()
  }

  // 매칭 성사 처리
  const handleMatchSuccess = (data: any) => {
    console.log('🎉 매칭 성사 처리:', data)
    
    // 매칭 ID 저장
    matchingState.currentMatchId.value = data.matchId || 'default-match-id'
    
    // 매칭 상태로 변경
    matchingState.setMatched()
    
    // 수락 타이머 시작
    matchingState.startAcceptTimer()
    
    // 모달 데이터 준비
    const modalData: MatchModalData = {
      topicTitle: data.topicTitle || '매칭된 주제',
      stanceText: data.stanceText || '선택된 진영',
      mode: data.mode || '1:1',
      topicId: data.topicId || 1
    }
    
    // 테스트용 사용자 데이터
    const users: MatchUser[] = [
      { userId: '유저1', stance: 'option1', accept: true, timestamp: Date.now() },
      { userId: '유저2', stance: 'option1', accept: null, timestamp: Date.now() },
      { userId: '유저3', stance: 'option2', accept: true, timestamp: Date.now() },
      { userId: '유저4', stance: 'option2', accept: false, timestamp: Date.now() }
    ]
    
    // 모달 표시
    modals.showMatchCompleteModal(modalData, users)
    
    console.log('✅ 매칭 성사 처리 완료')
  }

  // 타임아웃 처리
  const handleTimeout = () => {
    console.log('⏰ 매칭 타임아웃')
    
    // 매칭 중지
    matchingState.stopMatching()
    
    // 타임아웃 모달 표시
    modals.showTimeoutModal()
    
    console.log('✅ 타임아웃 처리 완료')
  }

  // 에러 처리
  const handleError = (error: string) => {
    console.error('❌ 매칭 에러:', error)
    
    // 에러 상태 설정
    matchingState.setError(error)
    
    // 매칭 중지
    matchingState.stopMatching()
    
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