import { ref, computed } from 'vue'
import type { Ref } from 'vue'

export interface MatchModalData {
  topicTitle: string
  stanceText: string
  mode: string
  topicId: number
}

// 진영별 수락/거절 현황
export interface StanceAcceptance {
  option1: {
    accept: number  // 찬성 진영 수락 수
    reject: number  // 찬성 진영 거절 수
  }
  option2: {
    accept: number  // 반대 진영 수락 수
    reject: number  // 반대 진영 거절 수
  }
}

export interface ModalState {
  isStartModalOpen: boolean
  isTimeoutModalOpen: boolean
  isTopicChangeModalOpen: boolean
  isHourWarningModalOpen: boolean
  isMatchCompleteModalOpen: boolean
  isLoginRequiredModalOpen: boolean
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
    isMatchCompleteModalOpen: false,
    isLoginRequiredModalOpen: false
  })

  // 매칭 모달 데이터
  const matchModalData = ref<MatchModalData>({
    topicTitle: '',
    stanceText: '',
    mode: '',
    topicId: 1
  })

  // 진영별 수락/거절 현황
  const stanceAcceptance = ref<StanceAcceptance>({
    option1: {
      accept: 0,
      reject: 0
    },
    option2: {
      accept: 0,
      reject: 0
    }
  })

  // 전체 사용자 수 (모드에 따라)
  const totalCount = computed(() => {
    const mode = matchModalData.value.mode
    return mode === '1:1' ? 2 : 4
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

  const showMatchCompleteModal = (data: MatchModalData) => {
    // 데이터를 명시적으로 설정
    matchModalData.value = {
      topicTitle: data.topicTitle || '',
      stanceText: data.stanceText || '',
      mode: data.mode || '',
      topicId: data.topicId || 1
    }
    
    // 진영별 수락/거절 현황 초기화
    stanceAcceptance.value = {
      option1: {
        accept: 0,
        reject: 0
      },
      option2: {
        accept: 0,
        reject: 0
      }
    }
    
    // 모달 상태를 명시적으로 설정
    modalState.value = {
      ...modalState.value,
      isMatchCompleteModalOpen: true
    }
  }

  const hideMatchCompleteModal = () => {
    modalState.value.isMatchCompleteModalOpen = false
  }

  // 진영별 수락/거절 현황 업데이트
  const updateStanceAcceptance = (stance: string, accept: boolean) => {
    if (stance === 'option1' || stance === 'option2') {
      if (accept) {
        stanceAcceptance.value[stance as 'option1' | 'option2'].accept++
      } else {
        stanceAcceptance.value[stance as 'option1' | 'option2'].reject++
      }
    } else {
      console.warn('⚠️ 유효하지 않은 진영:', { stance, accept })
    }
  }

  // 모든 모달 닫기
  const hideAllModals = () => {
    modalState.value = {
      isStartModalOpen: false,
      isTimeoutModalOpen: false,
      isTopicChangeModalOpen: false,
      isHourWarningModalOpen: false,
      isMatchCompleteModalOpen: false,
      isLoginRequiredModalOpen: false
    }
    // 진영별 수락/거절 현황 초기화
    stanceAcceptance.value = {
      option1: {
        accept: 0,
        reject: 0
      },
      option2: {
        accept: 0,
        reject: 0
      }
    }
  }

  return {
    // 상태
    modalState,
    matchModalData,
    stanceAcceptance,
    totalCount,

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
    showMatchCompleteModal,
    hideMatchCompleteModal,
    updateStanceAcceptance,
    hideAllModals
  }
}

export function useMatchingModals() {
  if (!instance) {
    instance = createMatchingModals()
  }
  return instance
} 