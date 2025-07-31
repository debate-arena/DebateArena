import axios from 'axios'

// 인증 API 전용 axios 인스턴스
const authAxios = axios.create({
  baseURL: '',  // 상대 경로 사용하여 프록시 활용
  timeout: 10000,
  withCredentials: true, // 쿠키 포함
  headers: {
    'Content-Type': 'application/json',
  },
})

// 요청 인터셉터
authAxios.interceptors.request.use(
  (config) => {
    console.log('🔐 Auth API Request:', config.method?.toUpperCase(), config.url)
    return config
  },
  (error) => {
    console.error('❌ Auth Request Error:', error)
    return Promise.reject(error)
  }
)

// 응답 인터셉터
authAxios.interceptors.response.use(
  (response) => {
    console.log('✅ Auth API Response:', response.status, response.config.url)
    return response
  },
  (error) => {
    console.error('❌ Auth Response Error:', error.response?.status, error.message)
    
    if (error.response?.status === 401) {
      console.log('🔐 인증 에러 발생')
    }
    
    return Promise.reject(error)
  }
)

export interface User {
  id: string
  email: string
  nickname?: string    // 닉네임 (필수 프로필, 선택적 필드)
}

export interface LoginResponse {
  user: User
  message: string
}

export interface LogoutResponse {
  message: string
}

export interface NicknameCheckResponse {
  status: string
  data: {
    available: boolean
    message: string
  }
}

export interface NicknameSaveResponse {
  status: string
  data?: User
  message?: string
}

export interface AuthResponse {
  status: string
  data?: User
  message?: string
}

// 인증 관련 API 서비스
export const authAPI = {
  // 인증 상태 확인
  verifyAuth: async (): Promise<{ success: boolean; message?: string }> => {
    try {
      const response = await authAxios.get('/api/auth/verify')
      const data = response.data
      console.log('🔐 인증 상태 확인 응답:', data)
      return { 
        success: data.status === 'success',
        message: data.message || data.data || '인증 상태 확인 완료'
      }
    } catch (err: any) {
      console.error('❌ 인증 상태 확인 실패:', err.response?.data || err.message)
      return { 
        success: false,
        message: err.response?.data?.message || err.message || '인증 상태 확인 실패'
      }
    }
  },

  // 현재 로그인된 사용자 정보 조회
  getCurrentUser: async (): Promise<User> => {
    const response = await authAxios.get('/api/user/profile')
    const data = response.data
    if (data.status === 'success' && data.data) {
      return data.data
    }
    throw new Error(data.data || '사용자 정보를 가져올 수 없습니다.')
  },

  // 로그아웃
  logout: async (): Promise<LogoutResponse> => {
    const response = await authAxios.post('/api/auth/logout')
    const data = response.data
    if (data.status === 'success') {
      return { message: '로그아웃되었습니다.' }
    }
    throw new Error(data.data || '로그아웃 중 오류가 발생했습니다.')
  },

  // 닉네임 중복 확인
  checkNickname: async (nickname: string): Promise<NicknameCheckResponse> => {
    const response = await authAxios.get(`/api/user/nickname/check?nickname=${encodeURIComponent(nickname)}`)
    return response.data
  },

  // 닉네임 저장
  saveNickname: async (nickname: string): Promise<NicknameSaveResponse> => {
    console.log('🔍 닉네임 저장 요청:', nickname)
    const response = await authAxios.put('/api/user/nickname', { nickname })
    console.log('🔍 닉네임 저장 응답:', response.data)
    return response.data
  },

  // OAuth2 로그인 URL 생성
  getOAuthUrl: (provider: 'google', next?: string): string => {
    const baseUrl = '/oauth2/authorization/google'
    if (next) {
      return `${baseUrl}?next=${encodeURIComponent(next)}`
    }
    return baseUrl
  }
} 