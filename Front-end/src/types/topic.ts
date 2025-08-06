// 단일 토론 주제 타입
export interface Topic {
  id: number;         // 주제 고유 ID
  title: string;      // 주제 제목/내용
  option1: string;    // 선택지 1 (예: 찬성 등)
  option2: string;    // 선택지 2 (예: 반대 등)
}

// 5개 주제와 시작/종료 시각을 담는 세트 타입 (epoch ms)
export interface TopicSet {
  topics: Topic[];      // 5개 주제 배열
  startAtMs: number;    // 세트 시작 시각 (epoch ms)
  endAtMs: number;      // 세트 종료 시각 (epoch ms)
} 