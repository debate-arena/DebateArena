import { ref, computed } from 'vue'
import { useMatchingStore } from '@/store/matching'

// 싱글톤 인스턴스
let modalInstance: ReturnType<typeof createModalInstance> | null = null

function createModalInstance() {
  const matchingStore = useMatchingStore()
  
  // 모달 상태 (직접 ref 사용)
  const isStartModalOpen = ref(false)
  const isMatchCompleteModalOpen = ref(false)
  const isConnectingModalOpen = ref(false)
  const isTimeoutModalOpen = ref(false)
  const isHourWarningModalOpen = ref(false)
  const isTopicChangeModalOpen = ref(false)
  
  // 모달 데이터
  const matchInfo = ref<{
    topicTitle: string
    myStance: string
    mode: string
  }>({
    topicTitle: '',
    myStance: '',
    mode: ''
  })
  
  const roomInfo = ref<{
    roomId: string
    connectedUsers: number
    totalUsers: number
  }>({
    roomId: '',
    connectedUsers: 0,
    totalUsers: 2
  })

  // 모달 표시 함수들
  const showStartModal = () => {
    console.log('🔍 showStartModal 호출됨')
    isStartModalOpen.value = true
    console.log('✅ start 모달 상태:', isStartModalOpen.value)
  }

  const showMatchCompleteModal = (topicTitle: string, myStance: string, mode: string) => {
    console.log('🔍 showMatchCompleteModal 호출됨:', { topicTitle, myStance, mode })
    console.log('🔍 호출 전 모달 상태:', isMatchCompleteModalOpen.value)
    matchInfo.value = { topicTitle, myStance, mode }
    isMatchCompleteModalOpen.value = true
    console.log('🔍 호출 후 모달 상태:', isMatchCompleteModalOpen.value)
    console.log('✅ matchComplete 모달 상태:', isMatchCompleteModalOpen.value)
    console.log('✅ matchInfo 설정됨:', matchInfo.value)
  }

  const showConnectingModal = () => {
    console.log('🔍 showConnectingModal 호출됨')
    roomInfo.value = {
      roomId: `debate_room_${Date.now()}`,
      connectedUsers: 0,
      totalUsers: 2
    }
    isConnectingModalOpen.value = true
    console.log('✅ connecting 모달 상태:', isConnectingModalOpen.value)
  }

  const showTimeoutModal = () => {
    console.log('🔍 showTimeoutModal 호출됨')
    isTimeoutModalOpen.value = true
    console.log('✅ timeout 모달 상태:', isTimeoutModalOpen.value)
  }

  const showHourWarningModal = () => {
    console.log('🔍 showHourWarningModal 호출됨')
    isHourWarningModalOpen.value = true
    console.log('✅ hourWarning 모달 상태:', isHourWarningModalOpen.value)
  }

  const showTopicChangeModal = () => {
    console.log('🔍 showTopicChangeModal 호출됨')
    isTopicChangeModalOpen.value = true
    console.log('✅ topicChange 모달 상태:', isTopicChangeModalOpen.value)
  }

  // 모달 숨김 함수들
  const hideStartModal = () => {
    console.log('🔍 hideStartModal 호출됨')
    isStartModalOpen.value = false
  }

  const hideMatchCompleteModal = () => {
    console.log('🔍 hideMatchCompleteModal 호출됨')
    isMatchCompleteModalOpen.value = false
    if (matchingStore.isMatching) {
      matchingStore.cancelMatching()
    }
  }

  const hideConnectingModal = () => {
    console.log('🔍 hideConnectingModal 호출됨')
    isConnectingModalOpen.value = false
    if (matchingStore.isMatching) {
      matchingStore.cancelMatching()
    }
  }

  const hideTimeoutModal = () => {
    console.log('🔍 hideTimeoutModal 호출됨')
    isTimeoutModalOpen.value = false
  }

  const hideHourWarningModal = () => {
    console.log('🔍 hideHourWarningModal 호출됨')
    isHourWarningModalOpen.value = false
  }

  const hideTopicChangeModal = () => {
    console.log('🔍 hideTopicChangeModal 호출됨')
    isTopicChangeModalOpen.value = false
  }

  const hideAllModals = () => {
    console.log('🔍 hideAllModals 호출됨')
    isStartModalOpen.value = false
    isMatchCompleteModalOpen.value = false
    isConnectingModalOpen.value = false
    isTimeoutModalOpen.value = false
    isHourWarningModalOpen.value = false
    isTopicChangeModalOpen.value = false
    if (matchingStore.isMatching) {
      matchingStore.cancelMatching()
    }
  }

  // 모달 열림 상태 확인
  const isAnyModalOpen = computed(() => {
    return isStartModalOpen.value || 
           isMatchCompleteModalOpen.value || 
           isConnectingModalOpen.value || 
           isTimeoutModalOpen.value ||
           isHourWarningModalOpen.value ||
           isTopicChangeModalOpen.value
  })

  return {
    // 모달 상태
    isStartModalOpen,
    isMatchCompleteModalOpen,
    isConnectingModalOpen,
    isTimeoutModalOpen,
    isHourWarningModalOpen,
    isTopicChangeModalOpen,
    
    // 모달 데이터
    matchInfo,
    roomInfo,
    
    // 모달 표시 함수
    showStartModal,
    showMatchCompleteModal,
    showConnectingModal,
    showTimeoutModal,
    showHourWarningModal,
    showTopicChangeModal,
    
    // 모달 숨김 함수
    hideStartModal,
    hideMatchCompleteModal,
    hideConnectingModal,
    hideTimeoutModal,
    hideHourWarningModal,
    hideTopicChangeModal,
    hideAllModals,
    
    // 유틸리티
    isAnyModalOpen
  }
}

/**
 * 매칭 모달 관리 Composable
 * - 모든 모달의 상태 관리
 * - 모달 표시/숨김 함수 제공
 */
export function useMatchingModals() {
  if (!modalInstance) {
    modalInstance = createModalInstance()
  }
  return modalInstance
} 