import axios from 'axios'

// 매칭 API 전용 axios 인스턴스
const matchingAxios = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  withCredentials: true, // 쿠키 포함
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

export interface Topic {
  id: number
  title: string
  option1: string
  option2: string
}

export interface TopicSet {
  topics: Topic[]
  startAtMs: number
  endAtMs: number
}

export interface TopicSetResponse {
  currentSet: TopicSet
  nextSet: TopicSet | null
  serverTimeMs: number
}

// 매칭 관련 API 서비스
export const matchingAPI = {
  // 주제 세트 조회
  getTopicSets: async (): Promise<TopicSetResponse> => {
    const response = await matchingAxios.get('/api/matching/topics')
    return response.data
  },

  // 매칭 시작
  startMatching: async (data: {
    topicId: number
    playerMode: string
    stance: string
  }) => {
    const response = await matchingAxios.post('/api/matching/start', data)
    return response.data
  },

  // 매칭 취소
  cancelMatching: async () => {
    const response = await matchingAxios.post('/api/matching/cancel')
    return response.data
  },

  // 매칭 상태 확인
  getMatchingStatus: async () => {
    const response = await matchingAxios.get('/api/matching/status')
    return response.data
  }
} 