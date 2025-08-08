import { ref, computed } from 'vue'
import type { Ref } from 'vue'

// MatchModalData와 StanceAcceptance는 더 이상 사용하지 않음 (useMatchResultState로 이동됨)

export interface ModalState {
  isStartModalOpen: boolean
  isTimeoutModalOpen: boolean
  isTopicChangeModalOpen: boolean
  isHourWarningModalOpen: boolean
  isLoginRequiredModalOpen: boolean
  // isMatchCompleteModalOpen 제거됨 (MatchResultPanel로 대체)
}

// 싱글톤 인스턴스
let instance: ReturnType<typeof createMatchingModals> | null = null

function createMatchingModals() {
  // 모달 상태 관리
  const modalState = ref<ModalState>({
    isStartModalOpen: false,
    isTimeoutModalOpen: false,
    isTopicChangeModalOpen: false,
    isHourWarningModalOpen: false,
    isLoginRequiredModalOpen: false
  })





  // 모달 표시 함수들
  const showStartModal = () => {
    modalState.value.isStartModalOpen = true
  }

  const hideStartModal = () => {
    modalState.value.isStartModalOpen = false
  }

  const showTimeoutModal = () => {
    modalState.value.isTimeoutModalOpen = true
  }

  const hideTimeoutModal = () => {
    modalState.value.isTimeoutModalOpen = false
  }

  const showTopicChangeModal = () => {
    modalState.value.isTopicChangeModalOpen = true
  }

  const hideTopicChangeModal = () => {
    modalState.value.isTopicChangeModalOpen = false
  }

  const showHourWarningModal = () => {
    modalState.value.isHourWarningModalOpen = true
  }

  const hideHourWarningModal = () => {
    modalState.value.isHourWarningModalOpen = false
  }

  const showLoginRequiredModal = () => {
    modalState.value.isLoginRequiredModalOpen = true
  }

  const hideLoginRequiredModal = () => {
    modalState.value.isLoginRequiredModalOpen = false
  }



  // 모든 모달 닫기
  const hideAllModals = () => {
    modalState.value = {
      isStartModalOpen: false,
      isTimeoutModalOpen: false,
      isTopicChangeModalOpen: false,
      isHourWarningModalOpen: false,
      isLoginRequiredModalOpen: false
    }

  }

  return {
    // 상태
    modalState,
    
    // 모달 제어 함수들
    showStartModal,
    hideStartModal,
    showTimeoutModal,
    hideTimeoutModal,
    showTopicChangeModal,
    hideTopicChangeModal,
    showHourWarningModal,
    hideHourWarningModal,
    showLoginRequiredModal,
    hideLoginRequiredModal,
    hideAllModals

  }
}

export function useMatchingModals() {
  if (!instance) {
    instance = createMatchingModals()
  }
  return instance
} 