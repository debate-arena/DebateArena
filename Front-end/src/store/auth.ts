import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI, type User } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // 상태
  const user = ref<User | null>(null)
  const isAuthenticated = ref(false)
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const isLoggingOut = ref(false) // 로그아웃 중인지 확인하는 플래그

  // 게터
  const isLoggedIn = computed(() => isAuthenticated.value && user.value !== null)
  const userNickname = computed(() => user.value?.nickname || '')
  const userEmail = computed(() => user.value?.email || '')
  const hasNickname = computed(() => !!user.value?.nickname)

  // 액션
  const checkAuthStatus = async () => {
    isLoading.value = true
    error.value = null
    
    try {
      // 1. 먼저 인증 상태 확인
      const authResult = await authAPI.verifyAuth()
      console.log('🔍 인증 상태 확인:', authResult)
      
      if (authResult.success) {
        // 2. 인증된 경우에만 프로필 정보 조회
        const userData = await authAPI.getCurrentUser()
        console.log('🔍 프로필 정보 조회 성공:', userData)
        user.value = userData
        isAuthenticated.value = true
        console.log('✅ 백엔드 메시지:', authResult.message)
      } else {
        // 3. 인증되지 않은 경우 상태 초기화
        console.log('🔍 인증되지 않음 - 상태 초기화')
        console.log('❌ 백엔드 메시지:', authResult.message)
        user.value = null
        isAuthenticated.value = false
      }
    } catch (err) {
      console.error('🔍 인증 상태 확인 실패:', err)
      // 인증 실패 시 명확하게 상태 초기화
      user.value = null
      isAuthenticated.value = false
      error.value = null // 에러 메시지 제거 (로그아웃 상태는 정상)
    } finally {
      isLoading.value = false
    }
  }

  const clearError = () => {
    error.value = null
  }

  // 로그인 성공 처리 (OAuth2 콜백에서 호출)
  const handleLoginSuccess = async () => {
    try {
      await checkAuthStatus()
    } catch (err) {
      console.error('로그인 성공 처리 오류:', err)
    }
  }

  // 로그인 실패 처리
  const handleLoginError = (message: string = '로그인에 실패했습니다.') => {
    error.value = message
  }

  // 닉네임 중복 확인
  const checkNickname = async (nickname: string): Promise<boolean> => {
    try {
      const response = await authAPI.checkNickname(nickname)
      return response.status === 'success' && response.data.available
    } catch (err) {
      console.error('닉네임 중복 확인 오류:', err)
      return false
    }
  }

  // 닉네임 저장
  const saveNickname = async (nickname: string): Promise<boolean> => {
    try {
      const response = await authAPI.saveNickname(nickname)
      if (response.status === 'success' && response.data) {
        user.value = response.data
        return true
      }
      return false
    } catch (err) {
      console.error('닉네임 저장 오류:', err)
      return false
    }
  }

  // 로그아웃
  const logout = async () => {
    isLoading.value = true
    isLoggingOut.value = true // 로그아웃 시작
    
    try {
      await authAPI.logout()
      console.log('🔍 로그아웃 성공')
    } catch (err) {
      console.error('🔍 로그아웃 오류:', err)
    } finally {
      // 로그아웃 시 모든 상태 강제 초기화
      user.value = null
      isAuthenticated.value = false
      error.value = null
      isLoading.value = false
      isLoggingOut.value = false // 로그아웃 완료
      
      console.log('🔍 로그아웃 완료 - 상태 초기화됨')
    }
  }

  return {
    // 상태
    user,
    isAuthenticated,
    isLoading,
    error,
    isLoggingOut,
    
    // 게터
    isLoggedIn,
    userNickname,
    userEmail,
    hasNickname,
    
    // 액션
    checkAuthStatus,
    clearError,
    handleLoginSuccess,
    handleLoginError,
    checkNickname,
    saveNickname,
    logout,
  }
}) 