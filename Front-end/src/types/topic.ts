// 단일 토론 주제 타입
export interface Topic {
  id: number;         // 주제 고유 ID
  title: string;      // 주제 제목/내용
  option1: string;    // 선택지 1 (예: 찬성 등)
  option2: string;    // 선택지 2 (예: 반대 등)
  index: number;      // 서버 순서 인덱스 (0부터 시작)
}

// 주제 세트 타입 (시간 정보는 currentSet에만 포함)
export interface TopicSet {
  topics: Topic[];      // 주제 배열
  remainingTimeSeconds?: number;  // 남은 시간 (초) - currentSet에만 있음
} 