import type { Stance, PlayerMode } from '@/types/matching'

/**
 * 진영 텍스트 변환
 */
export function getStanceText(topicId: number, stance: Stance): string {
  switch (stance) {
    case 'option1':
      return '찬성'
    case 'option2':
      return '반대'
    case 'random':
      return '상관없음'
    default:
      return '미선택'
  }
}

/**
 * 주제 제목 조회
 */
export function getTopicTitle(topicId: number, topicSetStore: any): string {
  const topic = topicSetStore.currentSet?.topics.find((t: any) => t.id === topicId)
  return topic?.title || `주제 ${topicId}`
}

/**
 * 예상 대기 시간 포맷팅
 */
export function formatEstimatedTime(minutes: number): string {
  if (minutes < 1) {
    return '~1분'
  } else if (minutes < 5) {
    return `~${minutes}분`
  } else {
    return '5분 이상'
  }
}

/**
 * 타이머 포맷팅 (mm:ss)
 */
export function formatTimer(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
}

/**
 * 모드 순서 정렬 (1:1이 항상 앞에 오도록)
 */
export function sortModes(modes: PlayerMode[]): PlayerMode[] {
  return modes.sort((a, b) => {
    if (a === '1:1') return -1
    if (b === '1:1') return 1
    return a.localeCompare(b)
  })
} 