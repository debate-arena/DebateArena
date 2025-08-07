import axios from 'axios'

// 토론방 API 전용 axios 인스턴스
const roomAxios = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 요청 인터셉터
roomAxios.interceptors.request.use(
  (config) => {
    console.log('🏠 Room API Request:', config.method?.toUpperCase(), config.url)
    return config
  },
  (error) => {
    console.error('❌ Room Request Error:', error)
    return Promise.reject(error)
  }
)

// 응답 인터셉터
roomAxios.interceptors.response.use(
  (response) => {
    console.log('✅ Room API Response:', response.status, response.config.url)
    return response
  },
  (error) => {
    console.error('❌ Room Response Error:', error.response?.status, error.message)
    return Promise.reject(error)
  }
)

export interface Room {
  id: string
  name: string
  topic: string
  participants: number
  maxParticipants: number
  status: 'waiting' | 'active' | 'finished'
  createdAt: string
}

export interface CreateRoomRequest {
  name: string
  topic: string
  maxParticipants: number
}

export interface JoinRoomRequest {
  roomId: string
}

// 토론방 관련 API 서비스
export const roomAPI = {
  // 토론방 목록 조회
  getRooms: async (): Promise<Room[]> => {
    const response = await roomAxios.get('/api/rooms')
    return response.data
  },

  // 토론방 생성
  createRoom: async (data: CreateRoomRequest): Promise<Room> => {
    const response = await roomAxios.post('/api/rooms', data)
    return response.data
  },

  // 토론방 참여
  joinRoom: async (data: JoinRoomRequest): Promise<Room> => {
    const response = await roomAxios.post('/api/rooms/join', data)
    return response.data
  },

  // 토론방 나가기
  leaveRoom: async (roomId: string): Promise<void> => {
    await roomAxios.post(`/api/rooms/${roomId}/leave`)
  },

  // 토론방 정보 조회
  getRoomInfo: async (roomId: string): Promise<Room> => {
    const response = await roomAxios.get(`/api/rooms/${roomId}`)
    return response.data
  }
} 