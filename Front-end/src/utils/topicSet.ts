import type { TopicSetStatusType } from '@/constants/topicSet';

export function getServerNowMs(clientNowMs: number, serverTimeOffsetMs: number): number {
  return clientNowMs + serverTimeOffsetMs;
}

export function getRemainingMs(endAtMs: number, serverNowMs: number): number {
  return Math.max(0, endAtMs - serverNowMs);
}

// 상태 판단 타입: SWAP, REFETCH, NONE (MVP, prefetch 완전 제거)
export type DecideActionType = 'SWAP' | 'REFETCH' | 'NONE';

/**
 * decideAction: 현재 상태/시간에 따라 어떤 액션을 취할지 결정 (MVP)
 * - SWAP: remainMs <= 0 && hasNextSet
 * - REFETCH: remainMs <= 0 && !hasNextSet
 * - NONE: 아무것도 안 함
 */
export function decideAction(params: {
  remainMs: number;
  status: TopicSetStatusType;
  hasNextSet: boolean;
}): DecideActionType {
  if (params.remainMs <= 0) {
    if (params.hasNextSet) return 'SWAP';
    return 'REFETCH';
  }
  return 'NONE';
}

/**
 * shouldResync: 정각 N초 전 등 임계 구간에서 서버 시간 재보정 필요 여부 판단
 */
export function shouldResync(remainMs: number, thresholdMs: number, lastResyncMs: number, nowMs: number): boolean {
  return remainMs <= thresholdMs && nowMs - lastResyncMs > thresholdMs;
} 