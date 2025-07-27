import { useMatchingStore } from '@/store/matching'
import { useWebSocket } from '@/composables/useWebSocket'
import { useMatchingModals } from '@/composables/useMatchingModals'
import type { Stance, PlayerMode } from '@/types/matching'

// 싱글톤 인스턴스
let actionsInstance: ReturnType<typeof createActionsInstance> | null = null

function createActionsInstance() {
  const matchingStore = useMatchingStore()
  const { sendMatchingRequest, sendMatchingCancel } = useWebSocket()
  const { showMatchCompleteModal, showTimeoutModal, showTopicChangeModal } = useMatchingModals()

  // 매칭 시작
  const startMatching = (selections: { topicId: number; stance: Stance; modes: PlayerMode[] }[], onTimeout?: () => void) => {
    console.log('🚀 startMatching 호출됨:', selections)
    
    const success = sendMatchingRequest(selections)
    
    if (success) {
      matchingStore.startMatching()
      console.log('✅ 매칭 상태 업데이트됨')
      
      // 가상 매칭 시뮬레이션 (3초 후 결과)
      setTimeout(() => {
        // 매칭이 이미 취소되었으면 처리하지 않음
        if (!matchingStore.isMatching) {
          console.log('❌ 매칭이 이미 취소되어 시뮬레이션 중단')
          return
        }
        
        // 확률 조정: 매칭 성공 50%, 타임아웃 25%, 주제 변경 25%
        const randomValue = Math.random()
        const isSuccess = randomValue < 0.5        // 50% 성공
        const isTimeout = randomValue >= 0.5 && randomValue < 0.75  // 25% 타임아웃
        const isHourlyTimeout = randomValue >= 0.75  // 25% 주제 변경
        
        console.log('🎲 확률 결과:', { randomValue, isSuccess, isTimeout, isHourlyTimeout })
        
        if (isSuccess) {
          console.log('🎉 매칭 성사!')
          // 가상 매칭 정보 생성
          const selectedItem = matchingStore.selectedTopicSelections[0]
          const topicTitle = selectedItem ? `주제 ${selectedItem.topicId}` : '알 수 없음'
          const myStance = selectedItem ? selectedItem.stance : 'random'
          const mode = selectedItem?.modes[0] || '1:1'
          handleMatchSuccess(topicTitle, myStance, mode)
        } else if (isHourlyTimeout) {
          console.log('🔄 주제 변경으로 인한 취소')
          showTopicChangeModal()
          // onTimeout은 호출하지 않음 (주제 변경은 별도 처리)
        } else {
          console.log('⏰ 매칭 타임아웃 (10분 초과)')
          if (onTimeout) {
            onTimeout()
          }
        }
      }, 3000) // 3초 후 결과
      
      return true
    } else {
      console.error('❌ 매칭 요청 전송 실패')
      return false
    }
  }

  // 매칭 취소
  const cancelMatching = (stopTimer?: () => void) => {
    console.log('🚫 cancelMatching 호출됨')
    
    if (stopTimer) {
      stopTimer()
    }
    
    const success = sendMatchingCancel()
    
    if (success) {
      matchingStore.cancelMatching()
      console.log('✅ 매칭 취소 완료')
      return true
    } else {
      console.error('❌ 매칭 취소 전송 실패')
      return false
    }
  }

  // 매칭 성사 처리
  const handleMatchSuccess = (topicTitle: string, myStance: string, mode: string) => {
    console.log('🎉 handleMatchSuccess 호출됨:', { topicTitle, myStance, mode })
    
    // 매칭 상태를 먼저 업데이트하여 중복 호출 방지
    matchingStore.cancelMatching()
    console.log('✅ 매칭 상태 취소됨')
    
    // 모달 표시
    showMatchCompleteModal(topicTitle, myStance, mode)
    console.log('✅ 매칭 성사 모달 표시 요청 완료')
  }

  // 타임아웃 처리
  const handleTimeout = (onTimeout: () => void) => {
    console.log('⏰ handleTimeout 호출됨')
    
    // 이미 매칭이 취소되었으면 처리하지 않음
    if (!matchingStore.isMatching) {
      console.log('❌ 매칭이 이미 취소되어 타임아웃 처리 중단')
      return
    }
    
    // 웹소켓 취소 요청
    sendMatchingCancel()
    
    // 매칭 상태 업데이트
    matchingStore.cancelMatching()
    console.log('✅ 매칭 상태 취소됨')
    
    // 타임아웃 모달 표시
    showTimeoutModal()
    console.log('✅ 타임아웃 모달 표시 요청 완료')
    onTimeout() // 타임아웃 발생 시 콜백 호출
  }

  // 매칭 시뮬레이션
  const simulateMatching = (onTimeout: () => void) => {
    console.log('🎲 매칭 시뮬레이션 시작')
    
    // 확률 조정: 매칭 성공 50%, 타임아웃 25%, 주제 변경 25%
    const randomValue = Math.random()
    const isSuccess = randomValue < 0.5        // 50% 성공
    const isTimeout = randomValue >= 0.5 && randomValue < 0.75  // 25% 타임아웃
    const isHourlyTimeout = randomValue >= 0.75  // 25% 주제 변경
    
    console.log('🎲 확률 결과:', { randomValue, isSuccess, isTimeout, isHourlyTimeout })
    
    setTimeout(() => {
      if (isSuccess) {
        console.log('✅ 매칭 성공!')
        const selectedItem = matchingStore.selectedTopicSelections[0]
        const topicTitle = selectedItem ? `주제 ${selectedItem.topicId}` : '알 수 없음'
        const myStance = selectedItem ? selectedItem.stance : 'random'
        const mode = selectedItem?.modes[0] || '1:1'
        handleMatchSuccess(topicTitle, myStance, mode)
      } else if (isTimeout) {
        console.log('⏰ 타임아웃 발생')
        handleTimeout(onTimeout)
      } else if (isHourlyTimeout) {
        console.log('🔄 주제 변경으로 인한 취소')
        showTopicChangeModal()
        // onTimeout() 호출하지 않음 - 주제 변경은 별도 처리
      }
    }, 3000) // 3초 후 결과
  }

  return { startMatching, cancelMatching, handleMatchSuccess, handleTimeout }
}

/**
 * 매칭 액션 관리 Composable
 * - 매칭 시작/취소
 * - 매칭 성사/타임아웃 처리
 */
export function useMatchingActions() {
  if (!actionsInstance) {
    actionsInstance = createActionsInstance()
  }
  return actionsInstance
} 