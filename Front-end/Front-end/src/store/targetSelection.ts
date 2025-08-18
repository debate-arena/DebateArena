import { defineStore } from 'pinia'

export interface DebateSelectTargetResponse {
  attacker: string
  defender: string
}

type UserId = string

export const useTargetSelectionStore = defineStore('targetSelection', {
  state: () => ({
    // 공격자 -> 수비자
    selections: new Map<UserId, UserId>(),
    // 수비자 -> 공격자들
    selectedBy: new Map<UserId, Set<UserId>>(),
    roundId: 0,
    expiresAt: 0,      // 서버 기준 종료 시각(ms). 없으면 0
    isLocked: false,   // 종료 후 true
  }),
  getters: {
    attackersFor: (s) => (defender: UserId) =>
      Array.from(s.selectedBy.get(defender) ?? []),
  },
  actions: {
    startWindow({ roundId, expiresAt }: { roundId: number; expiresAt: number }) {
      this.roundId = roundId
      this.expiresAt = expiresAt
      this.isLocked = false
      this.selections.clear()
      this.selectedBy.clear()
    },
    lock() { this.isLocked = true },

    applySelect({ attacker, defender }: DebateSelectTargetResponse) {
      if (this.isLocked || (this.expiresAt && Date.now() > this.expiresAt)) return

      const prev = this.selections.get(attacker)
      if (prev === defender) return // 동일 선택 반복 방어

      // 이전 수비자 목록에서 제거
      if (prev) {
        const prevSet = this.selectedBy.get(prev)
        if (prevSet) {
          prevSet.delete(attacker)
          if (prevSet.size === 0) this.selectedBy.delete(prev)
        }
      }

      // 새 수비자에 추가
      this.selections.set(attacker, defender)
      let set = this.selectedBy.get(defender)
      if (!set) {
        set = new Set<UserId>()
        this.selectedBy.set(defender, set)
      }
      set.add(attacker)
    },

    // 공격자가 선택을 해제하거나 방을 떠났을 때
    clearSelectionFor(attacker: UserId) {
      const prev = this.selections.get(attacker)
      if (!prev) return
      const set = this.selectedBy.get(prev)
      if (set) {
        set.delete(attacker)
        if (set.size === 0) this.selectedBy.delete(prev)
      }
      this.selections.delete(attacker)
    },

    // 늦게 들어온 사용자 동기화용
    syncSnapshot(snapshot: Record<UserId, UserId>) {
      this.selections.clear()
      this.selectedBy.clear()
      for (const [att, def] of Object.entries(snapshot)) {
        this.selections.set(att, def)
        let set = this.selectedBy.get(def)
        if (!set) {
          set = new Set<UserId>()
          this.selectedBy.set(def, set)
        }
        set.add(att)
      }
    },
  },
})
