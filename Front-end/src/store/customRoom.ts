import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useAuthStore } from '@/store/auth'
import type { ParticipantDisplay } from '@/types/participant'

export type CustomMode = '1:1' | '2:2'

export interface CustomRoomParticipant {
  email: string
  nickname: string
  side: 'L' | 'R' | null
  ready: boolean
}

export interface CustomRoomState {
  roomId: string | null
  topic: string
  leftTeamName: string
  rightTeamName: string
  mode: CustomMode
  ownerEmail: string | null
  ownerNickname: string | null
  participants: CustomRoomParticipant[]
}

export const useCustomRoomStore = defineStore('customRoom', () => {
  const state = ref<CustomRoomState>({
    roomId: null,
    topic: '',
    leftTeamName: '좌측 진영',
    rightTeamName: '우측 진영',
    mode: '1:1',
    ownerEmail: null,
    ownerNickname: null,
    participants: [],
  })

  const auth = useAuthStore()

  const requiredPerSide = computed(() => (state.value.mode === '1:1' ? 1 : 2))
  const requiredTotal = computed(() => requiredPerSide.value * 2)

  const leftParticipants = computed(() => state.value.participants.filter(p => p.side === 'L'))
  const rightParticipants = computed(() => state.value.participants.filter(p => p.side === 'R'))
  const waitingParticipants = computed(() => state.value.participants.filter(p => p.side === null))

  const isOwner = computed(() => !!state.value.ownerEmail && state.value.ownerEmail === (auth.userEmail || ''))
  const allReady = computed(() => {
    if (state.value.participants.length !== requiredTotal.value) return false
    const leftOk = leftParticipants.value.length === requiredPerSide.value
    const rightOk = rightParticipants.value.length === requiredPerSide.value
    const everyoneReady = state.value.participants.every(p => p.ready && p.side !== null)
    return leftOk && rightOk && everyoneReady
  })

  function reset() {
    state.value = {
      roomId: null,
      topic: '',
      leftTeamName: '1팀',
      rightTeamName: '2팀',
      mode: '1:1',
      ownerEmail: null,
      ownerNickname: null,
      participants: [],
    }
  }

  function createRoom(params: { topic: string; leftTeamName: string; rightTeamName: string; mode: CustomMode; roomId?: string }) {
    const roomId = params.roomId ?? crypto.randomUUID()
    const email = auth.userEmail || 'guest@example.com'
    const nickname = auth.userNickname || '게스트'
    state.value.roomId = roomId
    state.value.topic = params.topic
    state.value.leftTeamName = params.leftTeamName
    state.value.rightTeamName = params.rightTeamName
    state.value.mode = params.mode
    state.value.ownerEmail = email
    state.value.ownerNickname = nickname
    state.value.participants = [
      { email, nickname, side: null, ready: false },
    ]
    return roomId
  }

  function hydrateRoom(data: CustomRoomState) {
    state.value = { ...data, ownerNickname: data.ownerNickname ?? null }
  }

  function joinRoom(roomId: string, payload?: Partial<Pick<CustomRoomState, 'topic' | 'leftTeamName' | 'rightTeamName' | 'mode' | 'ownerEmail' | 'ownerNickname'>>) {
    // 프론트 전용 기본 합류 (백엔드 연동 시 교체)
    if (!state.value.roomId) {
      state.value.roomId = roomId
      state.value.topic = payload?.topic ?? state.value.topic
      state.value.leftTeamName = payload?.leftTeamName ?? state.value.leftTeamName
      state.value.rightTeamName = payload?.rightTeamName ?? state.value.rightTeamName
      state.value.mode = payload?.mode ?? state.value.mode
      state.value.ownerEmail = payload?.ownerEmail ?? state.value.ownerEmail
      state.value.ownerNickname = payload?.ownerNickname ?? state.value.ownerNickname
    }
    const email = auth.userEmail || 'guest@example.com'
    const nickname = auth.userNickname || '게스트'
    if (!state.value.participants.find(p => p.email === email)) {
      state.value.participants.push({ email, nickname, side: null, ready: false })
    }
  }

  function selectSide(email: string, side: 'L' | 'R') {
    const me = state.value.participants.find(p => p.email === email)
    if (!me) return
    // 준비 완료 상태면 변경 불가
    if (me.ready) return

    const sideList = side === 'L' ? leftParticipants.value : rightParticipants.value
    if (sideList.length >= requiredPerSide.value) return
    me.side = side
  }

  function toggleReady(email: string, ready?: boolean) {
    const me = state.value.participants.find(p => p.email === email)
    if (!me) return
    // 사이드 선택이 먼저 필요
    if (!me.side) return
    me.ready = ready !== undefined ? ready : !me.ready
  }

  function backToWaiting(email: string) {
    const me = state.value.participants.find(p => p.email === email)
    if (!me) return
    me.side = null
    me.ready = false
  }

  function toRoomDisplay(): { roomId: string; participants: ParticipantDisplay[] } | null {
    if (!state.value.roomId) return null
    return {
      roomId: state.value.roomId,
      participants: state.value.participants
        .filter(p => p.side !== null)
        .map(p => ({
          userId: p.email,
          displayName: p.nickname,
          side: p.side as 'L' | 'R',
        })),
    }
  }

  return {
    state,
    // getters
    requiredPerSide,
    requiredTotal,
    leftParticipants,
    rightParticipants,
    waitingParticipants,
    isOwner,
    allReady,
    // actions
    reset,
    createRoom,
    hydrateRoom,
    joinRoom,
    selectSide,
    toggleReady,
    backToWaiting,
    toRoomDisplay,
  }
})


