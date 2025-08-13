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