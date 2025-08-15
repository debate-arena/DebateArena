export interface Team {
  order: number;
  user: string;
}

export interface Speaker {
  id: number;
  name: string;
  team: 'left' | 'right';
  profileImage: string;
  isCurrent: boolean;
  hasSpoken: boolean;
}

export interface DebateSession {
  currentSpeakerId: number | null;
  nextSpeakerId: number | null; // 다음 발언자 미리 표시용
  speakingTimeLimit: number; // 초 단위
  remainingTime: number;
  speakingOrder: number[];
  currentOrderIndex: number;
  stage: 'waiting' | 'speaking' | 'transition' | 'finished'; // transition 단계 추가
  transitionTimeLeft: number; // 3초 대기시간
}

// 서버에서 오는 메시지 타입들
export interface SpeakerChangeMessage {
  type: 'speaker_change';
  currentSpeakerId: number | null;
  nextSpeakerId: number | null;
  remainingTime: number;
  transitionTimeLeft?: number;
}

export interface STTMessage {
  type: 'stt_result';
  speakerId: number;
  text: string;
  isInterim: boolean;
  timestamp: number;
}

export interface SessionUpdateMessage {
  type: 'session_update';
  stage: 'waiting' | 'speaking' | 'transition' | 'finished';
  currentOrderIndex: number;
  speakingOrder: number[];
}

export interface DebateStartMessage {
  roomId: number;
  type: number;
  topicId: number;
  topicText: string;
  firstOption: string;
  secondOption: string;
  status: string;
  webRTCStatus: string;
  debateStartAt: string;
  firstTeam: Team[];
  secondTeam: Team[];
}

export interface DebateSpeakStartMessage {
  speaker: string;
  speakerStartAt: string;
}

export interface DebateSpeakEndMessage {
  speaker: string;
  speakerEndAt: string;
  nextSpeaker: string;
}

export interface DebateSelectTargetMessage {
  status: 'success';
  battleStartAt: string;
}

export interface DebateSelectTargetResponse {
  attacker: string;
  defender: string;
}

export interface DebateVoteStartMessage {
  voteStartAt: string;
}

export interface DebateVoteEndMessage {
  voteInfo: Record<string, number>; // 투표 안한 사람은 안뜸
  voteResult: number, // 0: 좌측 진영, 1: 우측 진영, 2: 무승부
  voteEndAt: string;
}

export interface DebateChatMessage {
  nickname: string;
  message: string;
  team: number; // 0: 좌측 진영, 1: 우측 진영, 2: 관전자
}