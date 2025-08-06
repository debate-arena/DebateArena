import { defineStore } from 'pinia';
import type { TopicSet } from '@/types/topic';
import {
  TopicSetStatus,
  type TopicSetStatusType,
} from '@/constants/topicSet';
import { topicAPI, type ServerTopicsResponse } from '@/api/topics';

/**
 * Pinia Store: 토론 세트 상태 관리 (새로운 설계)
 * - status(상태머신) 하나로만 상태 표현 (READY, LOADING, SWAPPING, ERROR)
 * - 시간은 초 단위로 관리 (remainingTimeSeconds)
 * - 원자적 $patch, 상태 전이 완결성 보장
 *
 * 상태 전이 다이어그램:
 * INIT → LOADING → READY → SWAPPING → READY ...
 * (에러 발생 시 ERROR, 정상 fetch 성공 시 READY로 복귀)
 * 
 * TODO: Matching.vue에서 바꿔야 하는 부분들
 * 1. 주제 변경 타이머 표시: remainingTime → topicSetStore.remainingTimeSeconds
 * 2. 주제 카드 데이터: topicSetStore.currentSet?.topics 사용
 * 3. 주제 변경 타이머 시작: startTopicChangeTimer() 제거하고 useTopicSetController 사용
 * 4. 시간 표시 함수: formatTime(topicSetStore.remainingTimeSeconds) 사용
 * 5. 주제 초기화: onMounted에서 topicSetStore.fetchTopicSets() 호출
 */
export const useTopicSetStore = defineStore('topicSet', {
  state: () => ({
    currentSet: null as TopicSet | null,
    nextSet: null as TopicSet | null,
    remainingTimeSeconds: 0,  // 현재 세트 남은 시간 (초)
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
     * fetchTopicSets: 실제 API에서 주제 세트 데이터를 받아와서 저장
     * - status만으로 상태 표현, 원자적 $patch, 상태 전이 완결성
     */
    async fetchTopicSets() {
      if (this.status === TopicSetStatus.LOADING) return;
      this.status = TopicSetStatus.LOADING;
      this.error = null;
      try {
        // 실제 API 호출 (새로운 topicAPI 사용)
        const response: ServerTopicsResponse = await topicAPI.getCurrentTopics();
        
        if (response.status !== 'success') {
          // 서버에서 에러가 와도 클라이언트는 정상 동작
          console.log('⚠️ 서버에서 토픽을 가져올 수 없음 - 현재 토픽 유지');
          this.status = TopicSetStatus.READY;
          return;
        }

        const { topicList, timeToNextHour } = response.data;
        
        // 서버 응답을 클라이언트 타입으로 변환
        const convertServerTopics = (serverTopics: any[]) => {
          return serverTopics.map((topic, index) => ({
            id: topic.id,
            title: topic.topicText,
            option1: topic.firstOption,
            option2: topic.secondOption,
            index: index    // 서버 순서 인덱스 (0부터 시작)
          }));
        };

        // 현재 세트 (시간 정보 포함)
        const currentSet: TopicSet = {
          topics: convertServerTopics(topicList.currentTopics),
          remainingTimeSeconds: timeToNextHour,
        };

        // 다음 세트 (시간 정보 없음)
        const nextSet: TopicSet | null = topicList.nextTopics.length > 0 ? {
          topics: convertServerTopics(topicList.nextTopics),
          // remainingTimeSeconds 없음
        } : null;

        // 원자적 $patch로 상태 업데이트
        this.$patch({
          currentSet,
          nextSet,
          remainingTimeSeconds: timeToNextHour,
          status: TopicSetStatus.READY,
          error: null,
        });

        console.log('✅ 주제 세트 데이터 로드 완료:', {
          currentTopics: currentSet.topics.length,
          nextTopics: nextSet?.topics.length || 0,
          timeToNextHour,
          formattedTime: response.data.formattedTime
        });

      } catch (e) {
        console.error('❌ 주제 세트 로드 실패:', e);
        this.status = TopicSetStatus.ERROR;
        this.error = '주제 세트 데이터를 불러오지 못했습니다.';
      }
    },

    /**
     * swapSets: currentSet ← nextSet을 원자적으로 교체
     * - nextSet 없으면 즉시 fetchTopicSets()로 fallback
     * - 교체 후 즉시 서버에서 최신 데이터 가져오기
     */
    async swapSets() {
      if (!this.nextSet) {
        // nextSet 없으면 즉시 재요청 + 로딩
        await this.fetchTopicSets();
        return;
      }
      
      this.status = TopicSetStatus.SWAPPING;
      
      // 1. nextSet을 currentSet으로 교체 (시간 없이)
      this.$patch({
        currentSet: this.nextSet,
        nextSet: null,
        status: TopicSetStatus.READY,
        error: null,
      });
      
      // 2. 즉시 서버에서 시간 정보를 포함한 최신 데이터 가져오기
      await this.fetchTopicSets();
    },

    /**
     * 시간 감소: 1초씩 감소시키는 메서드
     */
    decrementTime() {
      if (this.remainingTimeSeconds > 0) {
        this.remainingTimeSeconds--;
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