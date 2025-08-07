/**
 * 매칭 관련 타입 정의
 */

// 인원수 모드
export type PlayerMode = '1:1' | '2:2'

// 진영 선택
export type Stance = 'option1' | 'option2' | 'random'

// 매칭 상태
export type MatchingStatus = 'idle' | 'waiting' | 'matching' | 'connecting' | 'completed' | 'timeout' | 'cancelled' | 'matched' | 'error'

// WebSocket 메시지 타입
export type WebSocketMessageType = 'MATCH_STATUS' | 'MATCH_INVITATION' | 'ACCEPTANCE_STATUS' | 'ERROR' | 'MATCH_ADDITIONAL' | 'MATCH_ALL_PERSONAL' | 'MATCH_RESULT'

// WebSocket 메시지 상태
export type WebSocketMessageStatus = 'success' | 'error' | 'warning' | 'info'

// WebSocket 메시지 인터페이스
export interface WebSocketMessage {
  type: WebSocketMessageType
  status: WebSocketMessageStatus
  data?: any
  message?: string
}

// 매칭 결과 데이터 타입 (새로 추가)
export interface MatchResultData {
  status: 'success' | 'fail' | 'error'
  data: {
    roomId: number | null
    message?: string
  }
}

export interface DebateRoomResponse {
  roomId: number | null
}

// 주제 정보 (매칭 페이지용)
export interface Topic {
  id: number
  title: string
  description: string
  tags: string[]
  waitingCount: {
    agree: number
    disagree: number
    random: number
  }
}

// 주제 선택 정보
export interface TopicSelection {
  topicId: number
  topicIndex: number  // 서버 순서 인덱스 (0부터 시작)
  stance: Stance
  modes: Set<PlayerMode>
  modeOrder: PlayerMode[] // 모드 선택 순서 관리
}

// 매칭 결과
export interface MatchResult {
  topicId: number
  stance: Stance
  mode: PlayerMode
  participants: string[]
  roomId: string
}

// 매칭 상태 관리
export interface MatchingState {
  // 주제별 선택 상태
  topicSelections: Map<number, TopicSelection>

  // 글로벌 선택 상태
  globalModes: Set<PlayerMode>
  globalStances: Set<Stance>

  // 매칭 상태
  isMatching: boolean
  status: MatchingStatus
  elapsedTime: number
  estimatedWaitTime?: number
  matchResult?: MatchResult

  // 오류 상태
  error?: string
} 