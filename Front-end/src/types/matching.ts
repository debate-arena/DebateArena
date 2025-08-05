/**
 * 매칭 관련 타입 정의
 */

// 인원수 모드
export type PlayerMode = '1:1' | '2:2'

// 진영 선택
export type Stance = 'option1' | 'option2' | 'random'

// 매칭 상태
export type MatchingStatus = 'idle' | 'waiting' | 'matching' | 'connecting' | 'completed' | 'timeout' | 'cancelled' | 'matched' | 'error'

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