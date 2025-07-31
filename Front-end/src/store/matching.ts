import { defineStore } from 'pinia'
import { useTopicSetStore } from './topicSet'
import type { TopicSelection, Stance, PlayerMode, MatchingStatus, MatchResult } from '@/types/matching'

interface MatchingState {
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

export const useMatchingStore = defineStore('matching', {
  state: (): MatchingState => ({
    topicSelections: new Map(),
    globalModes: new Set(['1:1', '2:2']), // 기본값
    globalStances: new Set(['random']),   // 기본값
    isMatching: false,
    status: 'idle',
    elapsedTime: 0,
    estimatedWaitTime: undefined,
    matchResult: undefined,
    error: undefined
  }),

  getters: {
    // 선택된 주제 개수
    selectedTopicCount: (state) => state.topicSelections.size,

    // 선택된 모드 개수
    selectedModeCount: (state) => {
      const allModes = new Set<PlayerMode>()
      state.topicSelections.forEach(selection => {
        selection.modes.forEach(mode => allModes.add(mode))
      })
      return allModes.size
    },

    // 매칭 시작 가능 여부
    canStartMatching: (state) => {
      // 주제 1개 이상, 모드 1개 이상, 매칭 중이 아닐 때만 true
      const hasMode = Array.from(state.topicSelections.values()).some(sel => sel.modes.size > 0)
      return state.topicSelections.size > 0 && hasMode && !state.isMatching
    },

    // 선택 요약 텍스트
    selectionSummary: (state) => {
      const topicCount = state.topicSelections.size
      const modes = Array.from(state.globalModes).join(', ') || '미선택'
      return `선택 ${topicCount}건 · 모드: ${modes}`
    },
    
    // 선택된 주제 선택 목록 (명확한 네이밍)
    selectedTopicSelections: (state) => {
      return Array.from(state.topicSelections.values()).map(selection => ({
        topicId: selection.topicId,
        stance: selection.stance,
        modes: Array.from(selection.modes) // Set을 Array로 변환
      }))
    },

    // 주제 정보 조회 getter
    getTopicById: (state) => (topicId: number) => {
      const topicSetStore = useTopicSetStore()
      return topicSetStore.currentSet?.topics.find(topic => topic.id === topicId)
    },

    // 글로벌 모드가 선택되었는지 확인
    isGlobalModeSelected: (state) => (mode: PlayerMode) => {
      return state.globalModes.has(mode)
    },

    // 글로벌 진영이 선택되었는지 확인
    isGlobalStanceSelected: (state) => (stance: Stance) => {
      return state.globalStances.has(stance)
    },

    // WebSocket API 요청으로 변환
    toMatchRequest: (state) => {
      const choices: Array<{
        matchType: number
        matchTitle: number
        choice: number
      }> = []
      
      state.topicSelections.forEach(selection => {
        selection.modes.forEach(mode => {
          choices.push({
            matchType: mode === '1:1' ? 0 : 1,
            matchTitle: selection.topicId - 1, // API는 0부터 시작
            choice: (() => {
              switch (selection.stance) {
                case 'option1': return 1  // pro (찬성)
                case 'option2': return 2  // con (반대)
                case 'random': return 0   // any (상관없음)
                default: return 0
              }
            })()
          })
        })
      })
      
      return { choices }
    }
  },

  actions: {
    // 주제 진영 선택
    selectTopicStance(topicId: number, stance: Stance) {
      const existing = this.topicSelections.get(topicId)
      
      if (existing) {
        existing.stance = stance
        this.topicSelections.set(topicId, existing)
      } else {
        this.createTopicSelection(topicId, stance)
      }
      
      this.syncGlobalState()
    },

    // 주제 모드 선택/해제
    toggleTopicMode(topicId: number, mode: PlayerMode) {
      const selection = this.topicSelections.get(topicId)
      if (!selection) return

      if (selection.modes.has(mode)) {
        // 모드 제거
        selection.modes.delete(mode)
        selection.modeOrder = selection.modeOrder.filter(m => m !== mode)
      } else {
        // 모드 추가
        selection.modes.add(mode)
        // 1:1이 항상 앞에 오도록 순서 관리
        if (mode === '1:1') {
          selection.modeOrder.unshift(mode)
        } else {
          selection.modeOrder.push(mode)
        }
      }

      // 모드가 없으면 선택 제거
      if (selection.modes.size === 0) {
        this.topicSelections.delete(topicId)
      }
      
      this.syncGlobalState()
    },

    // 글로벌 모드 토글
    toggleGlobalMode(mode: PlayerMode) {
      const hasMode = this.globalModes.has(mode)
      
      if (hasMode) {
        this.globalModes.delete(mode)
        // 모든 주제에서도 제거
        this.topicSelections.forEach(selection => {
          selection.modes.delete(mode)
          selection.modeOrder = selection.modeOrder.filter(m => m !== mode)
        })
      } else {
        this.globalModes.add(mode)
        // 모든 주제에도 추가
        this.topicSelections.forEach(selection => {
          selection.modes.add(mode)
          // 1:1이 항상 앞에 오도록 순서 관리
          if (mode === '1:1') {
            selection.modeOrder.unshift(mode)
          } else {
            selection.modeOrder.push(mode)
          }
        })
      }
    },

    // 글로벌 진영 설정
    setGlobalStance(stance: Stance) {
      this.globalStances.clear()
      this.globalStances.add(stance)
      
      // 모든 주제에 적용
      this.topicSelections.forEach(selection => {
        selection.stance = stance
      })
    },

    // 개별 선택 변경 시 글로벌 상태 동기화 (간결한 네이밍)
    syncGlobalState() {
      this.updateGlobalModes()
      this.updateGlobalStances()
    },

    // 글로벌 모드 상태 업데이트
    updateGlobalModes() {
      this.globalModes.clear()
      
      if (this.topicSelections.size === 0) return
      
      // 모든 주제가 공통으로 가지고 있는 모드 찾기
      const allModes = new Set<PlayerMode>()
      this.topicSelections.forEach(selection => {
        selection.modes.forEach(mode => allModes.add(mode))
      })
      
      allModes.forEach(mode => {
        const allHaveMode = Array.from(this.topicSelections.values()).every(selection => 
          selection.modes.has(mode)
        )
        if (allHaveMode) {
          this.globalModes.add(mode)
        }
      })
    },

    // 글로벌 진영 상태 업데이트
    updateGlobalStances() {
      this.globalStances.clear()
      
      if (this.topicSelections.size === 0) return
      
      const allStances = new Set<Stance>()
      this.topicSelections.forEach(selection => {
        allStances.add(selection.stance)
      })
      
      // 모든 주제가 동일한 진영을 가지고 있는지 확인
      if (allStances.size === 1) {
        this.globalStances.add(Array.from(allStances)[0])
      }
    },

    // 새로운 주제 선택 생성
    createTopicSelection(topicId: number, stance: Stance) {
      const initialModes = new Set<PlayerMode>(this.globalModes)
      const modeOrder = Array.from(initialModes).sort((a, b) => {
        if (a === '1:1') return -1
        if (b === '1:1') return 1
        return a.localeCompare(b)
      })
      
      this.topicSelections.set(topicId, {
        topicId,
        stance,
        modes: initialModes,
        modeOrder
      })
    },

    // 페이지 진입 시 모든 주제에 초기 선택 생성
    initializeTopicSelections(topicIds: number[]) {
      this.topicSelections.clear()
      
      // 글로벌 상태가 비어있으면 기본값 설정
      if (this.globalModes.size === 0) {
        this.globalModes = new Set(['1:1', '2:2'])
      }
      if (this.globalStances.size === 0) {
        this.globalStances = new Set(['random'])
      }
      
      topicIds.forEach(topicId => {
        this.topicSelections.set(topicId, {
          topicId,
          stance: Array.from(this.globalStances)[0] || 'random',
          modes: new Set(this.globalModes),
          modeOrder: Array.from(this.globalModes)
        })
      })
      this.syncGlobalState()
    },

    // 매칭 관련 액션들
    startMatching() {
      if (!this.canStartMatching) return
      
      this.isMatching = true
      this.status = 'waiting'
      this.elapsedTime = 0
      this.error = undefined
    },

    // 매칭 취소
    cancelMatching() {
      this.isMatching = false
      this.status = 'idle'
      this.elapsedTime = 0
      this.estimatedWaitTime = undefined
      this.matchResult = undefined
      this.error = undefined
      // clearAllSelections()는 주제 변경으로 인한 취소에서만 호출
      console.log('cancelMatching 내부 상태:', {
        isMatching: this.isMatching,
        status: this.status,
        elapsedTime: this.elapsedTime
      })
    },

    updateTimer() {
      if (this.isMatching) {
        this.elapsedTime += 1
      }
    },

    // 매칭 완료
    completeMatching(result: MatchResult) {
      this.isMatching = false
      this.status = 'completed'
      this.matchResult = result
    },

    // 연결 시작
    startConnecting() {
      this.status = 'connecting'
    },

    // 연결 완료
    completeConnection() {
      this.status = 'completed'
    },

    // 오류 설정
    setError(error: string) {
      this.error = error
      this.status = 'idle'
    },

    // 유틸리티 액션들
    removeTopicSelection(topicId: number) {
      this.topicSelections.delete(topicId)
    },

    clearAllSelections() {
      this.topicSelections.clear()
      this.globalModes.clear()
      this.globalStances.clear()
    },

    // 진영을 API choice로 변환
    convertStanceToChoice(stance: Stance): number {
      switch (stance) {
        case 'option1': return 0  // pro (찬성)
        case 'option2': return 1  // con (반대)
        case 'random': return 2   // any (상관없음)
        default: return 2
      }
    },

    // WebSocket 매칭 요청 전송
    async sendMatchRequest() {
      if (!this.canStartMatching) return
      
      const request = this.toMatchRequest
      console.log('🎯 매칭 요청 전송:', request)
      
      // WebSocket으로 요청 전송
      // stompClient.publish({
      //   destination: '/pub/match/request',
      //   body: JSON.stringify(request)
      // })
      
      this.isMatching = true
      this.status = 'waiting'
      this.elapsedTime = 0
      this.error = undefined
    }
  }
}) 