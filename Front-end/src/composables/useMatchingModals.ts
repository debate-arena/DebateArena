import { ref, computed, readonly } from 'vue'
import type { Ref } from 'vue'

export interface MatchModalData {
  topicTitle: string
  stanceText: string
  mode: string
  topicId: number
}

export interface MatchUser {
  userId: string
  stance: string
  accept: boolean | null
  timestamp: number
}

export interface ModalState {
  isStartModalOpen: boolean
  isTimeoutModalOpen: boolean
  isTopicChangeModalOpen: boolean
  isHourWarningModalOpen: boolean
  isMatchCompleteModalOpen: boolean
}

export function useMatchingModals() {
  // 모달 상태 관리
  const modalState = ref<ModalState>({
    isStartModalOpen: false,
    isTimeoutModalOpen: false,
    isTopicChangeModalOpen: false,
    isHourWarningModalOpen: false,
    isMatchCompleteModalOpen: false
  })

  // 매칭 모달 데이터
  const matchModalData = ref<MatchModalData>({
    topicTitle: '',
    stanceText: '',
    mode: '',
    topicId: 1
  })

  // 매칭 사용자들
  const matchUsers = ref<Map<string, MatchUser>>(new Map())

  // 연결된 사용자 수
  const connectedCount = computed(() => {
    return Array.from(matchUsers.value.values()).filter(user => user.accept === true).length
  })

  // 전체 사용자 수
  const totalCount = computed(() => {
    return matchUsers.value.size
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

  const showMatchCompleteModal = (data: MatchModalData, users: MatchUser[]) => {
    matchModalData.value = data
    matchUsers.value.clear()
    users.forEach(user => {
      matchUsers.value.set(user.userId, user)
    })
    modalState.value.isMatchCompleteModalOpen = true
  }

  const hideMatchCompleteModal = () => {
    modalState.value.isMatchCompleteModalOpen = false
    matchUsers.value.clear()
  }

  // 사용자 상태 업데이트
  const updateUser = (userId: string, updates: Partial<MatchUser>) => {
    const user = matchUsers.value.get(userId)
    if (user) {
      Object.assign(user, updates)
      matchUsers.value.set(userId, user)
    }
  }

  // 모든 모달 닫기
  const hideAllModals = () => {
    Object.keys(modalState.value).forEach(key => {
      (modalState.value as any)[key] = false
    })
    matchUsers.value.clear()
  }

  return {
    // 상태
    modalState: readonly(modalState),
    matchModalData: readonly(matchModalData),
    matchUsers: readonly(matchUsers),
    connectedCount,
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
    showMatchCompleteModal,
    hideMatchCompleteModal,
    updateUser,
    hideAllModals
  }
} 