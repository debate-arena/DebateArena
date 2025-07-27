import { defineStore } from 'pinia';
import type { TopicSet } from '@/types/topic';
import {
  TopicSetStatus,
  type TopicSetStatusType,
} from '@/constants/topicSet';

/**
 * Pinia Store: 토론 세트 상태 관리 (MVP, prefetch 제거)
 * - status(상태머신) 하나로만 상태 표현 (READY, LOADING, SWAPPING, ERROR)
 * - 모든 시간은 epoch ms로만 관리
 * - 원자적 $patch, 상태 전이 완결성 보장
 *
 * 상태 전이 다이어그램:
 * INIT → LOADING → READY → SWAPPING → READY ...
 * (에러 발생 시 ERROR, 정상 fetch 성공 시 READY로 복귀)
 */
export const useTopicSetStore = defineStore('topicSet', {
  state: () => ({
    currentSet: null as TopicSet | null,
    nextSet: null as TopicSet | null,
    serverTimeOffsetMs: 0,
    status: TopicSetStatus.INIT as TopicSetStatusType,
    error: null as string | null,
  }),
  getters: {
    isLoading: (state) => state.status === TopicSetStatus.LOADING,
    isSwapping: (state) => state.status === TopicSetStatus.SWAPPING,
    isError: (state) => state.status === TopicSetStatus.ERROR,
    isReady: (state) => state.status === TopicSetStatus.READY,
  },
  actions: {
    /**
     * fetchTopicSets: current+next+serverTime을 받아와서 epoch ms로 저장
     * - status만으로 상태 표현, 원자적 $patch, 상태 전이 완결성
     */
    async fetchTopicSets() {
      if (this.status === TopicSetStatus.LOADING) return;
      this.status = TopicSetStatus.LOADING;
      this.error = null;
      try {
        // mock 데이터 (epoch ms)
        const now = Date.now();
        // 오늘 날짜의 0시, 12시 타임스탬프 계산
        const today = new Date();
        today.setSeconds(0, 0);
        const year = today.getFullYear();
        const month = today.getMonth();
        const date = today.getDate();
        const start0 = new Date(year, month, date, 0, 0, 0, 0).getTime();   // 00:00
        const end12 = new Date(year, month, date, 12, 0, 0, 0).getTime();   // 12:00
        const end24 = new Date(year, month, date + 1, 0, 0, 0, 0).getTime(); // 다음날 00:00
        const futureEnd = now + 3600 * 1000; // 현재 시각 기준 1시간 뒤
        const mockResponse = {
          currentSet: {
            topics: [
              { id: 1, title: 'AI 규제는 필요한가?', option1: '필요하다', option2: '필요없다' },
              { id: 2, title: '원격근무, 효율적인가?', option1: '그렇다', option2: '아니다' },
              { id: 3, title: '대중교통 무료화 찬반', option1: '찬성', option2: '반대' },
              { id: 4, title: 'SNS 실명제 도입', option1: '도입', option2: '미도입' },
              { id: 5, title: '로봇이 인간 일자리 대체', option1: '대체한다', option2: '대체하지 않는다' },
            ],
            startAtMs: start0, // 오늘 00:00
            endAtMs: futureEnd,    // 현재 시각 기준 1시간 뒤
          },
          nextSet: {
            topics: [
              { id: 6, title: '우주여행 상용화', option1: '가능', option2: '불가능' },
              { id: 7, title: '플라스틱 사용 금지', option1: '금지', option2: '허용' },
              { id: 8, title: '온라인 수업 효과', option1: '효과적', option2: '비효과적' },
              { id: 9, title: '최저임금 인상', option1: '인상', option2: '동결' },
              { id: 10, title: '자율주행차 보급', option1: '필요', option2: '불필요' },
            ],
            startAtMs: end12,  // 오늘 12:00
            endAtMs: end24,    // 다음날 00:00
          },
          serverTimeMs: now,
        };
        const clientNow = Date.now();
        this.$patch({
          serverTimeOffsetMs: mockResponse.serverTimeMs - clientNow,
          currentSet: mockResponse.currentSet,
          nextSet: mockResponse.nextSet,
          status: TopicSetStatus.READY,
          error: null,
        });
      } catch (e) {
        this.status = TopicSetStatus.ERROR;
        this.error = '주제 세트 데이터를 불러오지 못했습니다.';
      }
    },
    /**
     * swapSets: currentSet ← nextSet을 원자적으로 교체
     * - nextSet 없으면 즉시 fetchTopicSets()로 fallback
     * - status 관리, $patch
     */
    async swapSets() {
      if (!this.nextSet) {
        // nextSet 없으면 즉시 재요청 + 로딩
        await this.fetchTopicSets();
        return;
      }
      this.status = TopicSetStatus.SWAPPING;
      // 원자적 $patch
      this.$patch({
        currentSet: this.nextSet,
        nextSet: null,
        status: TopicSetStatus.READY,
        error: null,
      });
    },
    /**
     * reSyncServerTime: 서버 시간 재요청해서 offset 재계산
     * - throttle 등은 composable에서 관리
     */
    async reSyncServerTime() {
      try {
        const clientNow = Date.now();
        const serverTimeMs = clientNow; // mock
        this.serverTimeOffsetMs = serverTimeMs - clientNow;
      } catch (e) {
        // 에러 무시(네트워크 불안정 등)
      }
    },
    /**
     * 에러 상태 복구(정상 fetch 성공 시 READY로 복귀)
     */
    clearError() {
      if (this.status === TopicSetStatus.ERROR) {
        this.status = TopicSetStatus.READY;
        this.error = null;
      }
    },
  },
}); 