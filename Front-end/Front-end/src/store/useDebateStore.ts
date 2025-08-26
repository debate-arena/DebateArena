import { defineStore } from 'pinia'
import { ref, computed, markRaw } from 'vue'

// 타입 정의
export interface Participant {
  email: string
  team: 'L' | 'R'
  isConnected: boolean
  isSpeaking: boolean
}

export interface STTChunk {
  speaker: string
  text: string
  timestamp: number
}

export type DebatePhase = 
  | 'waiting'      // 연결 대기
  | 'speaking'     // 발언 중
  | 'transition'   // 단계 전환
  | 'attack'       // 공방전
  | 'defense'      // 수비
  | 'voting'       // 투표
  | 'summary'      // 요약
  | 'end'          // 토론 종료

export type Team = 'L' | 'R' | null

export interface DebateResult {
  winner: Team
  scores: Record<string, number>
  // TODO: 추가 결과 데이터 구조 정의
}

export const useDebateStore = defineStore('debate', () => {
  // [1] 내 정보
  const myEmail = ref<string>('')
  const myTeam = ref<Team>(null)
  const isProducer = ref<boolean>(false)
  const isMuted = ref<boolean>(false)

  // [2] 토론방 정보
  const roomId = ref<string>('')
  const participants = ref<Participant[]>([])

  // [3] 토론 상태
  const phase = ref<DebatePhase>('waiting')
  const currentSpeaker = ref<string | null>(null)
  const timeRemaining = ref<number>(0)
  const nextActionTimestamp = ref<number>(0)

  // [4] STT / 오디오
  const sttChunks = ref<STTChunk[]>([])
  const remoteTracks = ref<Record<string, MediaStreamTrack>>({})

  // [5] 공방전 등 특수 상태
  const selectedTarget = ref<string | null>(null)
  const debateResult = ref<DebateResult | null>(null)

  // 추가 UI 상태
  const isConnecting = ref<boolean>(false)
  const connectionError = ref<string | null>(null)

  // Computed properties
  const myParticipant = computed(() => 
    participants.value.find(p => p.email === myEmail.value)
  )

  const teamAParticipants = computed(() => 
    participants.value.filter(p => p.team === 'L')
  )

  const teamBParticipants = computed(() => 
    participants.value.filter(p => p.team === 'R')
  )

  const allParticipantsConnected = computed(() => 
    participants.value.length > 0 && participants.value.every(p => p.isConnected)
  )

  const currentSpeakerParticipant = computed(() => 
    currentSpeaker.value ? participants.value.find(p => p.email === currentSpeaker.value) : null
  )

  const isMyTurn = computed(() => 
    currentSpeaker.value === myEmail.value
  )

  // Actions
  const actions = {
    // [1] 내 정보 관리
    setMyInfo(email: string, team: Team, producer: boolean = false) {
      myEmail.value = email
      myTeam.value = team
      isProducer.value = producer
    },

    toggleMute() {
      isMuted.value = !isMuted.value
      // TODO: WebRTC 오디오 트랙 mute/unmute 처리
    },

    setMuteState(muted: boolean) {
      isMuted.value = muted
      // TODO: WebRTC 오디오 트랙 mute/unmute 처리
    },

    // [2] 토론방 정보 관리
    setRoomId(id: string) {
      roomId.value = id
    },

    updateParticipants(newParticipants: Participant[]) {
      participants.value = newParticipants
    },

    addParticipant(participant: Participant) {
      const existingIndex = participants.value.findIndex(p => p.email === participant.email)
      if (existingIndex >= 0) {
        participants.value[existingIndex] = participant
      } else {
        participants.value.push(participant)
      }
    },

    removeParticipant(email: string) {
      const index = participants.value.findIndex(p => p.email === email)
      if (index >= 0) {
        participants.value.splice(index, 1)
      }
    },

    updateParticipantConnection(email: string, isConnected: boolean) {
      const participant = participants.value.find(p => p.email === email)
      if (participant) {
        participant.isConnected = isConnected
      }
    },

    updateParticipantSpeaking(email: string, isSpeaking: boolean) {
      // 모든 참가자의 발언 상태를 false로 설정
      participants.value.forEach(p => p.isSpeaking = false)
      
      // 지정된 참가자만 발언 상태로 설정
      const participant = participants.value.find(p => p.email === email)
      if (participant) {
        participant.isSpeaking = isSpeaking
      }
    },

    // [3] 토론 상태 관리
    setPhase(newPhase: DebatePhase) {
      phase.value = newPhase
    },

    setCurrentSpeaker(email: string | null) {
      currentSpeaker.value = email
      
      // 발언자 변경 시 참가자 발언 상태도 업데이트
      participants.value.forEach(p => p.isSpeaking = false)
      if (email) {
        const speaker = participants.value.find(p => p.email === email)
        if (speaker) {
          speaker.isSpeaking = true
        }
      }
    },

    setTimeRemaining(time: number) {
      timeRemaining.value = time
    },

    decreaseTimeRemaining() {
      if (timeRemaining.value > 0) {
        timeRemaining.value--
      }
    },

    setNextActionTimestamp(timestamp: number) {
      nextActionTimestamp.value = timestamp
    },

    // [4] STT / 오디오 관리
    addSTTChunk(chunk: STTChunk) {
      sttChunks.value.push(chunk)
      
      // TODO: 최대 개수 제한 (예: 최근 100개만 유지)
      if (sttChunks.value.length > 100) {
        sttChunks.value.shift()
      }
    },

    clearSTTChunks() {
      sttChunks.value = []
    },

    addRemoteTrack(email: string, track: MediaStreamTrack) {
      // markRaw를 사용하여 Vue의 반응성 시스템에서 제외
      remoteTracks.value[email] = markRaw(track)
    },

    removeRemoteTrack(email: string) {
      if (remoteTracks.value[email]) {
        delete remoteTracks.value[email]
      }
    },

    clearRemoteTracks() {
      remoteTracks.value = {}
    },

    // [5] 공방전 등 특수 상태 관리
    setSelectedTarget(email: string | null) {
      selectedTarget.value = email
    },

    setDebateResult(result: DebateResult) {
      debateResult.value = result
    },

    // 연결 상태 관리
    setConnecting(connecting: boolean) {
      isConnecting.value = connecting
    },

    setConnectionError(error: string | null) {
      connectionError.value = error
    },

    // 전체 상태 초기화
    resetDebateState() {
      // 내 정보는 유지하고 토론 관련 상태만 초기화
      participants.value = []
      phase.value = 'waiting'
      currentSpeaker.value = null
      timeRemaining.value = 0
      nextActionTimestamp.value = 0
      sttChunks.value = []
      remoteTracks.value = {}
      selectedTarget.value = null
      debateResult.value = null
      isConnecting.value = false
      connectionError.value = null
    },

    // 완전 초기화 (방 나가기 시)
    resetAll() {
      myEmail.value = ''
      myTeam.value = null
      isProducer.value = false
      isMuted.value = false
      roomId.value = ''
      this.resetDebateState()
    },

    // TODO: 추가 액션들
    // - 투표 관련 액션
    // - 채팅 메시지 관리
    // - 토론 타이머 관리
    // - WebRTC 연결 상태 관리
    // - 오디오 볼륨 조절
    // - 발언권 요청/승인
  }

  return {
    // State
    myEmail,
    myTeam,
    isProducer,
    isMuted,
    roomId,
    participants,
    phase,
    currentSpeaker,
    timeRemaining,
    nextActionTimestamp,
    sttChunks,
    remoteTracks,
    selectedTarget,
    debateResult,
    isConnecting,
    connectionError,

    // Computed
    myParticipant,
    teamAParticipants,
    teamBParticipants,
    allParticipantsConnected,
    currentSpeakerParticipant,
    isMyTurn,

    // Actions
    ...actions
  }
})

// 타입 추출을 위한 유틸리티
export type DebateStore = ReturnType<typeof useDebateStore>