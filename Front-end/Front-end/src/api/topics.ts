import axios from 'axios'
import { config } from '@/config/env'

// 주제 API 전용 axios 인스턴스 (메인 서버용)
const topicAxios = axios.create({
  baseURL: config.MAIN_API_URL,  // 메인 서버 URL
  timeout: 10000,
  withCredentials: true, // 쿠키 포함
  headers: {
    'Content-Type': 'application/json',
  },
})

// 요청 인터셉터
topicAxios.interceptors.request.use(
  (config) => {
    console.log('📚 Topic API Request:', config.method?.toUpperCase(), config.url)
    return config
  },
  (error) => {
    console.error('❌ Topic Request Error:', error)
    return Promise.reject(error)
  }
)

// 응답 인터셉터
topicAxios.interceptors.response.use(
  (response) => {
    console.log('✅ Topic API Response:', response.status, response.config.url)
    return response
  },
  (error) => {
    console.error('❌ Topic Response Error:', error.response?.status, error.message)
    return Promise.reject(error)
  }
)

// 서버 응답 타입 정의
export interface ServerTopic {
  id: number
  topicText: string
  firstOption: string
  secondOption: string
}

export interface ServerTopicList {
  currentTopics: ServerTopic[]
  nextTopics: ServerTopic[]
}

export interface ServerTopicsData {
  topicList: ServerTopicList
  timeToNextHour: number
  formattedTime: string
}

export interface ServerTopicsResponse {
  status: string
  data: ServerTopicsData
}

// 주제 관련 API 서비스
export const topicAPI = {
  // 주제 세트 조회
  getCurrentTopics: async (): Promise<ServerTopicsResponse> => {
    const response = await topicAxios.get('/api/topics/current')
    return response.data
  },
} 