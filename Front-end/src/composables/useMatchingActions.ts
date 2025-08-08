import { useMatchingStore } from '@/store/matching'
import { useWebSocket } from '@/composables/useWebSocket'
import { teamToStance } from '@/utils/matching'
import { useMatchingModals } from '@/composables/useMatchingModals'
import { useMatchingTimer } from '@/composables/useMatchingTimer'
import { useAuthStore } from '@/store/auth'
import { useTopicSetStore } from '@/store/topicSet'


export function useMatchingActions() {
  const matchingStore = useMatchingStore()
  const webSocket = useWebSocket()
  const modals = useMatchingModals()
  const authStore = useAuthStore()
  const topicSetStore = useTopicSetStore()
  const { startMatchingTimer, stopMatchingTimer, startAcceptTimer, stopAcceptTimer } = useMatchingTimer()

  // 데이터 매핑 함수들
  const getTopicTitle = (topicId: number): string => {
    const topic = matchingStore.getTopicById(topicId)
    return topic?.title || '알 수 없는 주제'
  }

  const getTeamText = (topicId: number, team: number): string => {
    const topic = matchingStore.getTopicById(topicId)
    
    if (!topic) return '알 수 없는 진영'
    
    // team 번호에 따라 선택지 반환
    return teamToStance(team) === 'option1' ? topic.option1 : topic.option2
  }

  const getModeText = (type: number): string => {
    return type === 0 ? '1:1' : '2:2'
  }

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
    handleTimeout,
    handleError,
    getTopicTitle,
    getTeamText,
    getModeText
    // acceptMatch, rejectMatch, handleMatchSuccess 제거됨 (MatchResultPanel에서 직접 처리)
  }
} 