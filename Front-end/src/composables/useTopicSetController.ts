import { onMounted, onUnmounted, watch } from 'vue';
import { useTopicSetStore } from '@/store/topicSet';
import {
  RESYNC_THRESHOLD_MS,
  TopicSetStatus,
} from '@/constants/topicSet';
import {
  getServerNowMs,
  getRemainingMs,
  decideAction,
  shouldResync,
  type DecideActionType,
} from '@/utils/topicSet';

/**
 * useTopicSetController (MVP, prefetch 제거)
 * - await fetchTopicSets() 이후 자기-보정 setTimeout 타이머 시작
 * - decideAction으로 SWAP → REFETCH 우선순위 일원화
 * - RESYNC_THRESHOLD_MS 구간에서 shouldResync로 재보정
 * - visibilitychange 시 즉시 tick() 실행
 * - 에러 복구 후 타이머 재가동(watch)
 * - tick 재진입 방지(ticking 가드)
 * - dev 환경에서만 로그 남기기(예시)
 * - 상태 비교는 TopicSetStatus 상수만 사용
 */
export function useTopicSetController() {
  const store = useTopicSetStore();
  let timer: ReturnType<typeof setTimeout> | null = null;
  let lastResync = 0;
  let ticking = false;

  // 자기-보정 타이머: tick 실행 후, 다음 1초 정각까지 남은 시간만큼 예약
  const scheduleNextTick = () => {
    const now = Date.now();
    const next = Math.ceil(now / 1000) * 1000;
    const delay = Math.max(0, next - now);
    timer = setTimeout(tick, delay);
  };

  // tick: 남은 시간/임계구간 체크 및 swap/refetch 결정
  const tick = async () => {
    if (ticking) return; // 재진입 방지
    ticking = true;
    try {
      if (store.status === TopicSetStatus.ERROR) {
        // 에러 상태면 타이머 중단
        if (timer) clearTimeout(timer);
        timer = null;
        ticking = false;
        return;
      }
      if (!store.currentSet) {
        ticking = false;
        scheduleNextTick();
        return;
      }
      const clientNowMs = Date.now();
      const serverNowMs = getServerNowMs(clientNowMs, store.serverTimeOffsetMs);
      const remainMs = getRemainingMs(store.currentSet.endAtMs, serverNowMs);

      // 정각 N초 전(예: 10초)에 1회 reSyncServerTime (shouldResync 활용)
      if (shouldResync(remainMs, RESYNC_THRESHOLD_MS, lastResync, clientNowMs)) {
        store.reSyncServerTime();
        lastResync = clientNowMs;
      }

      // swap/refetch 우선순위 통합
      const action: DecideActionType = decideAction({
        remainMs,
        status: store.status,
        hasNextSet: !!store.nextSet,
      });
      if (import.meta.env.DEV) {
        // dev 환경에서만 로그
        console.log('[tick]', { action, remainMs, status: store.status, offset: store.serverTimeOffsetMs });
      }
      if (action === 'SWAP') {
        await store.swapSets();
      } else if (action === 'REFETCH') {
        await store.fetchTopicSets();
      }
    } finally {
      ticking = false;
      scheduleNextTick();
    }
  };

  // 탭 복귀 시 서버 시간 재보정 + tick 1회 즉시 실행 (throttle 적용)
  const handleVisibility = () => {
    const now = Date.now();
    if (document.visibilityState === 'visible') {
      if (now - lastResync > RESYNC_THRESHOLD_MS) {
        store.reSyncServerTime();
        lastResync = now;
      }
      tick();
    }
  };

  // 에러 복구 후 타이머 재가동 (READY 전환 감시)
  watch(
    () => store.status,
    (newStatus, oldStatus) => {
      if (oldStatus === TopicSetStatus.ERROR && newStatus === TopicSetStatus.READY && !timer) {
        scheduleNextTick();
      }
    }
  );

  onMounted(async () => {
    await store.fetchTopicSets();
    scheduleNextTick();
    window.addEventListener('visibilitychange', handleVisibility);
  });

  onUnmounted(() => {
    if (timer) clearTimeout(timer);
    window.removeEventListener('visibilitychange', handleVisibility);
  });
} 