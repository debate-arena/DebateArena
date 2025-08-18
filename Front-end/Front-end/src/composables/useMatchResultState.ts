import { ref, computed } from 'vue'

// 진영별 수락/거절 현황 (모달이 아닌 일반 상태용)
export interface StanceAcceptance {
  option1: {
    accept: number  // 찬성 진영 수락 수
    reject: number  // 찬성 진영 거절 수
  }
  option2: {
    accept: number  // 반대 진영 수락 수
    reject: number  // 반대 진영 거절 수
  }
}

// 싱글톤 인스턴스
let instance: ReturnType<typeof createMatchResultState> | null = null

function createMatchResultState() {
  // 진영별 수락/거절 현황
  const stanceAcceptance = ref<StanceAcceptance>({
    option1: {
      accept: 0,
      reject: 0
    },
    option2: {
      accept: 0,
      reject: 0
    }
  })

  // 팀 정원(한 진영의 최대 인원). 모드에 따라 설정됨(예: 1:1 → 1, 2:2 → 2)
  const teamSize = ref<number>(0)

  // 최근 이벤트 디듀플(초경량)용 타임스탬프 저장 (stance x type)
  const lastEventAt = ref({
    option1: { accept: 0, reject: 0 },
    option2: { accept: 0, reject: 0 }
  })
  const DEDUP_WINDOW_MS = 250

  // 진영별 수락/거절 현황 업데이트
  const updateStanceAcceptance = (stance: string, accept: boolean) => {
    console.log('🎯 updateStanceAcceptance 호출:', { stance, accept })
    
    if (stance === 'option1' || stance === 'option2') {
      const key = accept ? 'accept' : 'reject'
      const now = Date.now()

      // 초단기 디듀플: 같은 타입 이벤트가 너무 촘촘히 오면 1건만 반영
      if (now - (lastEventAt.value[stance][key] || 0) < DEDUP_WINDOW_MS) {
        console.log('⏱️ 디듀플 윈도우 내 중복 이벤트 무시:', { stance, accept })
        return
      }
      lastEventAt.value[stance][key] = now

      // 카운트 증가
      const bucket = stanceAcceptance.value[stance]
      if (accept) {
        bucket.accept += 1
        console.log('✅ 수락 카운트 증가:', stanceAcceptance.value)
      } else {
        bucket.reject += 1
        console.log('❌ 거절 카운트 증가:', stanceAcceptance.value)
      }

      // 팀 정원을 초과하지 않도록 클램프
      const maxPerStance = Math.max(0, teamSize.value || 0)
      if (maxPerStance > 0) {
        const total = bucket.accept + bucket.reject
        if (total > maxPerStance) {
          // 초과분을 잘라냄: 거절을 우선 줄이고, 부족하면 수락을 줄임
          let overflow = total - maxPerStance
          if (bucket.reject >= overflow) {
            bucket.reject -= overflow
          } else {
            overflow -= bucket.reject
            bucket.reject = 0
            bucket.accept = Math.max(0, bucket.accept - overflow)
          }
        }
      }
    } else {
      console.warn('⚠️ 유효하지 않은 진영:', { stance, accept })
    }
  }

  // 상태 초기화
  const resetStanceAcceptance = () => {
    stanceAcceptance.value = {
      option1: { accept: 0, reject: 0 },
      option2: { accept: 0, reject: 0 }
    }
    lastEventAt.value = { option1: { accept: 0, reject: 0 }, option2: { accept: 0, reject: 0 } }
    teamSize.value = 0
    console.log('🔄 StanceAcceptance 초기화됨')
  }

  // 팀 정원 설정(모드 변경 또는 초대 수신 시 호출)
  const setTeamSize = (size: number) => {
    teamSize.value = Math.max(0, Math.floor(size || 0))
  }

  return {
    // 상태
    stanceAcceptance,
    teamSize,
    
    // 액션
    updateStanceAcceptance,
    resetStanceAcceptance,
    setTeamSize
  }
}

// 싱글톤 패턴으로 사용
export const useMatchResultState = () => {
  if (!instance) {
    instance = createMatchResultState()
  }
  return instance
}

