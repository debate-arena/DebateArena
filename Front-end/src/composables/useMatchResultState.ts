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

  // 진영별 수락/거절 현황 업데이트
  const updateStanceAcceptance = (stance: string, accept: boolean) => {
    console.log('🎯 updateStanceAcceptance 호출:', { stance, accept })
    
    if (stance === 'option1' || stance === 'option2') {
      if (accept) {
        stanceAcceptance.value[stance as 'option1' | 'option2'].accept++
        console.log('✅ 수락 카운트 증가:', stanceAcceptance.value)
      } else {
        stanceAcceptance.value[stance as 'option1' | 'option2'].reject++
        console.log('❌ 거절 카운트 증가:', stanceAcceptance.value)
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
    console.log('🔄 StanceAcceptance 초기화됨')
  }

  return {
    // 상태
    stanceAcceptance,
    
    // 액션
    updateStanceAcceptance,
    resetStanceAcceptance
  }
}

// 싱글톤 패턴으로 사용
export const useMatchResultState = () => {
  if (!instance) {
    instance = createMatchResultState()
  }
  return instance
}

