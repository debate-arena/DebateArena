import { ref, computed, reactive } from 'vue'
import type { ModalState, ModalType, ModalEvent, MatchInfo, RoomInfo, User } from '@/types/modal'

// 싱글톤 인스턴스
let modalManagerInstance: ReturnType<typeof createModalManager> | null = null

function createModalManager() {
  // 모달 상태
  const modalState = reactive<ModalState>({
    isStartModalOpen: false,
    isMatchCompleteModalOpen: false,
    isConnectingModalOpen: false,
    isTimeoutModalOpen: false,
    isHourWarningModalOpen: false,
    isTopicChangeModalOpen: false
  })

  // 모달 데이터
  const matchInfo = ref<MatchInfo>({
    topicTitle: '',
    stanceText: '',
    mode: '',
    topicId: 0
  })

  const roomInfo = ref<RoomInfo>({
    roomId: '',
    connectedUsers: 0,
    totalUsers: 0
  })

  const users = ref<Map<string, User>>(new Map())

  // 이벤트 핸들러
  const eventHandlers = ref<Map<ModalType, (event: ModalEvent) => void>>(new Map())

  // 모달 표시 함수들
  const showModal = (type: ModalType, data?: any) => {
    console.log(`🔍 showModal 호출됨: ${type}`, data)
    
    switch (type) {
      case 'start':
        modalState.isStartModalOpen = true
        break
      case 'matchComplete':
        if (data) {
          matchInfo.value = data
        }
        modalState.isMatchCompleteModalOpen = true
        break
      case 'connecting':
        if (data) {
          roomInfo.value = data
        }
        modalState.isConnectingModalOpen = true
        break
      case 'timeout':
        modalState.isTimeoutModalOpen = true
        break
      case 'hourWarning':
        modalState.isHourWarningModalOpen = true
        break
      case 'topicChange':
        modalState.isTopicChangeModalOpen = true
        break
    }
    
    console.log(`✅ ${type} 모달 상태:`, modalState[`is${type.charAt(0).toUpperCase() + type.slice(1)}ModalOpen` as keyof ModalState])
  }

  const hideModal = (type: ModalType) => {
    console.log(`🔍 hideModal 호출됨: ${type}`)
    
    switch (type) {
      case 'start':
        modalState.isStartModalOpen = false
        break
      case 'matchComplete':
        modalState.isMatchCompleteModalOpen = false
        break
      case 'connecting':
        modalState.isConnectingModalOpen = false
        break
      case 'timeout':
        modalState.isTimeoutModalOpen = false
        break
      case 'hourWarning':
        modalState.isHourWarningModalOpen = false
        break
      case 'topicChange':
        modalState.isTopicChangeModalOpen = false
        break
    }
  }

  const hideAllModals = () => {
    console.log('🔍 hideAllModals 호출됨')
    Object.keys(modalState).forEach(key => {
      modalState[key as keyof ModalState] = false
    })
  }

  // 이벤트 핸들러 등록
  const onModalEvent = (type: ModalType, handler: (event: ModalEvent) => void) => {
    eventHandlers.value.set(type, handler)
  }

  // 이벤트 발생
  const emitModalEvent = (type: ModalType, event: ModalEvent) => {
    console.log(`🎯 모달 이벤트 발생: ${type}`, event)
    const handler = eventHandlers.value.get(type)
    if (handler) {
      handler(event)
    }
  }

  // 사용자 관리
  const setUsers = (newUsers: User[]) => {
    users.value.clear()
    newUsers.forEach(user => {
      users.value.set(user.userId, user)
    })
  }

  const updateUser = (userId: string, updates: Partial<User>) => {
    const user = users.value.get(userId)
    if (user) {
      users.value.set(userId, { ...user, ...updates })
    }
  }

  // 유틸리티
  const isAnyModalOpen = computed(() => {
    return Object.values(modalState).some(isOpen => isOpen)
  })

  const getConnectedCount = () => {
    return Array.from(users.value.values())
      .filter(user => user.accept === true).length
  }

  const getTotalCount = () => {
    return users.value.size
  }

  return {
    // 상태
    modalState,
    matchInfo,
    roomInfo,
    users,
    
    // 함수
    showModal,
    hideModal,
    hideAllModals,
    onModalEvent,
    emitModalEvent,
    setUsers,
    updateUser,
    
    // 유틸리티
    isAnyModalOpen,
    getConnectedCount,
    getTotalCount
  }
}

/**
 * 개선된 모달 관리 Composable
 * - 타입 안전한 모달 상태 관리
 * - 이벤트 기반 모달 핸들링
 * - 사용자 상태 관리
 */
export function useModalManager() {
  if (!modalManagerInstance) {
    modalManagerInstance = createModalManager()
  }
  return modalManagerInstance
} 