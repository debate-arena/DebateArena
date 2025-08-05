import axios from 'axios'
import { config } from '@/config/env'

// 매칭 API 전용 axios 인스턴스
const matchingAxios = axios.create({
  baseURL: config.MATCH_API_URL,  // 매칭 서버 URL
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 요청 인터셉터
matchingAxios.interceptors.request.use(
  (config) => {
    console.log('🎯 Matching API Request:', config.method?.toUpperCase(), config.url)
    return config
  },
  (error) => {
    console.error('❌ Matching Request Error:', error)
    return Promise.reject(error)
  }
)

// 응답 인터셉터
matchingAxios.interceptors.response.use(
  (response) => {
    console.log('✅ Matching API Response:', response.status, response.config.url)
    return response
  },
  (error) => {
    console.error('❌ Matching Response Error:', error.response?.status, error.message)
    return Promise.reject(error)
  }
)

// 매칭 관련 API 서비스
export const matchingAPI = {
  // 매칭 요청 (HTTP API 사용 시)
  sendMatchRequest: async (request: any) => {
    const response = await matchingAxios.post('/api/match/request', request)
    return response.data
  },

  // 매칭 응답 (HTTP API 사용 시)
  sendMatchAcceptance: async (matchId: string, accept: boolean, stance: number) => {
    const response = await matchingAxios.post('/api/match/acceptance', { matchId, accept, stance })
    return response.data
  },

  // 매칭 상태 조회
  getMatchStatus: async () => {
    const response = await matchingAxios.get('/api/match/status')
    return response.data
  }
} 