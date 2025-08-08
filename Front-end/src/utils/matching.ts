import type { Stance, PlayerMode } from '@/types/matching'
import type { Topic } from '@/types/topic'

/**
 * 진영 텍스트 변환 (주제별 실제 선택지 사용)
 */
export function getStanceText(stance: Stance, topic?: Topic): string {
  if (topic && stance !== 'random') {
    switch (stance) {
      case 'option1':
        return topic.option1 || '찬성'
      case 'option2':
        return topic.option2 || '반대'
    }
  }
  
  // fallback 또는 random인 경우
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
 * 팀 번호를 진영으로 변환
 */
export function teamToStance(team: number): Stance {
  return team === 0 ? 'option1' : 'option2'
}

/**
 * 유연한 팀 → 진영 변환 (서버 포맷 다양성 대응)
 * 허용 입력: 0/1, 1/2, '0'/'1', '1'/'2', 'PRO'/'CON', 'pro'/'con', 'option1'/'option2'
 */
export function teamToStanceFlexible(team: unknown): Stance {
  if (typeof team === 'number') {
    if (team === 0) return 'option1'
    if (team === 1) return 'option2'
    if (team === 2) return 'option2'
  }
  if (typeof team === 'string') {
    const t = team.toLowerCase()
    if (t === '0') return 'option1'
    if (t === '1' || t === '2') return 'option2'
    if (t === 'pro' || t === 'option1' || t === 'agree' || t === '찬성') return 'option1'
    if (t === 'con' || t === 'option2' || t === 'disagree' || t === '반대') return 'option2'
  }
  // 알 수 없는 값이면 기본값(option1)
  return 'option1'
}

/**
 * 진영 → 팀 번호(0|1) 변환
 */
export function stanceToTeam(stance: Stance): 0 | 1 {
  return stance === 'option1' ? 0 : 1
}

/**
 * 다양한 키로 전달되는 팀 정보를 읽어 0|1로 정규화
 */
export function normalizeTeamNumber(payload: any): 0 | 1 {
  const raw = payload?.team ?? payload?.userTeam ?? payload?.teamNumber ?? payload?.position ?? payload?.side
  const stance = teamToStanceFlexible(raw)
  return stanceToTeam(stance)
}

/**
 * 모드별 총 인원 수 계산
 */
export function getTotalCount(mode: PlayerMode): number {
  return mode === '1:1' ? 2 : 4
}

/**
 * 진영 버튼 스타일 클래스
 */
export function getStanceButtonClass(stance: Stance): string {
  switch (stance) {
    case 'option1':
      return 'bg-debate-left hover:bg-debate-left/90 text-slate-800 border-debate-left'
    case 'option2':
      return 'bg-debate-right hover:bg-debate-right/90 text-white border-debate-right'
    case 'random':
      return 'bg-debate-random hover:bg-debate-random/90 text-slate-700 border-debate-random'
    default:
      return 'bg-background hover:bg-accent'
  }
}

/**
 * 모드 버튼 스타일 클래스
 */
export function getModeButtonClass(mode: PlayerMode): string {
  switch (mode) {
    case '1:1':
      return 'bg-mode-1v1 hover:bg-indigo-200 active:bg-indigo-300 text-mode-1v1 border-mode-1v1 ring-mode-1v1'
    case '2:2':
      return 'bg-mode-2v2 hover:bg-mode-2v2/90 text-mode-2v2 border-mode-2v2 ring-mode-2v2'
    default:
      return 'bg-background hover:bg-accent'
  }
}

/**
 * 진영 배지 스타일 클래스
 */
export function getStanceBadgeClass(stance: Stance): string {
  switch (stance) {
    case 'option1':
      return 'bg-debate-left text-slate-800 border-debate-left'
    case 'option2':
      return 'bg-debate-right text-white border-debate-right'
    case 'random':
      return 'bg-debate-random text-slate-700 border-debate-random'
    default:
      return 'bg-slate-600 text-slate-300 border-slate-500'
  }
}

/**
 * 모드 배지 스타일 클래스
 */
export function getModeBadgeClass(mode: PlayerMode): string {
  switch (mode) {
    case '1:1':
      return 'bg-mode-1v1 text-slate-800 border-mode-1v1'
    case '2:2':
      return 'bg-mode-2v2 text-slate-800 border-mode-2v2'
    default:
      return 'bg-slate-600 text-slate-300 border-slate-500'
  }
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

/**
 * 매칭 인비테이션 데이터 처리 및 변환
 */
export function processMatchInvitation(data: any, topicSetStore: any) {
  const invitationData = data.data || {}
  
  const matchId = invitationData.matchId
  const invitationTopicId = invitationData.topicId
  const invitationTeam = invitationData.team || 0
  const invitationType = invitationData.type || 0
  
  // 서버 데이터를 UI 텍스트로 변환
  const invitationMode: '1:1' | '2:2' = invitationType === 0 ? '1:1' : '2:2'
  const invitationStance = teamToStanceFlexible(invitationTeam)
  
  // 토픽 ID 타입 변환 (string -> number 또는 number -> number)
  const topicId = typeof invitationTopicId === 'string' ? parseInt(invitationTopicId) : invitationTopicId
  const invitationTopicTitle = topicSetStore.currentSet?.topics.find((t: any) => t.id === topicId)?.title || '매칭된 주제'
  
  return {
    matchId,
    topicId,
    team: invitationTeam,
    stance: invitationStance,
    mode: invitationMode,
    topicTitle: invitationTopicTitle
  }
}

/**
 * 매칭 결과 데이터 처리 및 변환
 */
export function processMatchResult(data: any) {
  const resultData = data.data
  
  if (resultData && (resultData.status === 'success' || resultData.roomId)) {
    const roomId = resultData.roomId || resultData.data?.roomId
    return {
      success: true,
      roomId: roomId?.toString(),
      topicId: resultData.topicId,
      stance: resultData.stance,
      mode: resultData.mode
    }
  }
  
  return {
    success: false,
    roomId: null,
    error: '매칭 결과 데이터가 올바르지 않습니다.'
  }
}

/**
 * 수락/거절 상태 업데이트 처리
 */
export function processAcceptanceStatus(data: any) {
  const accept = data.data?.accept
  const team = data.data?.team || 0
  const stance = teamToStanceFlexible(team)
  
  return {
    accept,
    team,
    stance
  }
}

/**
 * 매칭 요청 데이터 검증
 */
export function validateMatchRequest(request: any): { valid: boolean; error?: string } {
  if (!request.topicId) {
    return { valid: false, error: '토픽 ID가 필요합니다.' }
  }
  
  if (!request.mode) {
    return { valid: false, error: '모드가 필요합니다.' }
  }
  
  if (!request.stance) {
    return { valid: false, error: '진영이 필요합니다.' }
  }
  
  return { valid: true }
} 