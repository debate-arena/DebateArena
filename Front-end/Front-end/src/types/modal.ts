export interface User {
  userId: string
  stance?: string
  accept: boolean | null
  timestamp: number
}

export interface MatchInfo {
  topicTitle: string
  stanceText: string
  mode: string
  topicId: number
}

export interface RoomInfo {
  roomId: string
  connectedUsers: number
  totalUsers: number
}

export interface ModalState {
  isStartModalOpen: boolean
  isMatchCompleteModalOpen: boolean
  isConnectingModalOpen: boolean
  isTimeoutModalOpen: boolean
  isHourWarningModalOpen: boolean
  isTopicChangeModalOpen: boolean
}

export type ModalType = 
  | 'start'
  | 'matchComplete'
  | 'connecting'
  | 'timeout'
  | 'hourWarning'
  | 'topicChange'

export interface ModalEvent {
  type: 'accept' | 'reject' | 'close' | 'confirm' | 'cancel'
  data?: any
} 