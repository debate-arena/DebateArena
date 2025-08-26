export const TopicSetStatus = {
  INIT: 'INIT',
  LOADING: 'LOADING',
  READY: 'READY',
  SWAPPING: 'SWAPPING',
  ERROR: 'ERROR',
} as const;
export type TopicSetStatusType = typeof TopicSetStatus[keyof typeof TopicSetStatus];

export const RESYNC_THRESHOLD_MS = 10 * 1000;  // 10초 전 재보정 