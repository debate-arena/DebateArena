// src/utils/debateResult.ts
import type { VoteDecidedResponse, AIJudgmentResponse } from '@/composables/useSpeechRecognition';

/**
 * /summaries/result 응답 파서 유틸
 *
 * 형태 1) 사람 투표로 이미 승부가 난 경우 (AI는 최종 요약만 제공)
 * {
 *   "full_summarize": { num1: "...", num2: "..." }
 * }
 *
 * 형태 2) 사람 무승부 → AI가 최종 판정한 경우
 * {
 *   "result": {
 *     "winner": "num1" | "num2" | "tie",
 *     "votes": { "num1": number, "num2": number, "none"?: number } // ✅ AI 청중단 집계표
 *     // soft_scores, details 등은 표시 안 함
 *   },
 *   "juror_explain": "대표 청중(배심원) 설명",
 *   "full_summarize": { num1: "...", num2: "..." }                  // 최종 요약(루트)
 * }
 */

/** 화면에서 쓰는 파싱 결과 타입들 */
export type ParsedVoteDecided = {
  type: 'vote_decided';
  num1Summary: string;
  num2Summary: string;
};

export type ParsedAIJudgment = {
  type: 'ai_judgment';
  aiWinner: 'num1' | 'num2' | 'tie' | null;
  /** 🧠 AI 청중단 판정 결과(집계표) = result.votes */
  aiVotes: { num1: number; num2: number } | null;
  /** 대표 청중(배심원) 설명 텍스트 */
  judgmentReason: string;
  /** 승/패 팀 최종 요약 */
  num1Summary: string;
  num2Summary: string;
};

/** 투표 승부 응답(요약만)인지 판단 */
export function isVoteDecided(payload: any): payload is VoteDecidedResponse {
  return (
    payload &&
    payload.full_summarize &&
    !payload.result &&
    typeof payload.full_summarize === 'object' &&
    !!payload.full_summarize.num1 &&
    !!payload.full_summarize.num2
  );
}

/** AI 판정 응답(사람 무승부)인지 판단 */
export function isAIJudgment(payload: any): payload is AIJudgmentResponse {
  return (
    payload &&
    payload.result &&
    typeof payload.result === 'object' &&
    !!payload.result.winner
  );
}

/** 투표 승부 데이터 추출 (요약만) */
export function extractVoteDecidedData(payload: VoteDecidedResponse): ParsedVoteDecided {
  const summarize = payload.full_summarize || {};
  return {
    type: 'vote_decided',
    num1Summary: summarize.num1 || '',
    num2Summary: summarize.num2 || '',
  };
}

/** AI 판정 데이터 추출 (승리/집계표/근거/요약) */
export function extractAIJudgmentData(payload: AIJudgmentResponse): ParsedAIJudgment {
  const result = payload.result || {};
  // ✅ 화면 표시용: AI 청중단 집계표 = result.votes
  const votes = result.votes ?? null;

  // 최종 요약(루트) — 호환겸 result.full_summarize 폴백
  const summarize =
    payload.full_summarize ??
    (result as any).full_summarize ??
    {};

  // 판정 근거(루트 우선, result 폴백)
  const reason =
    (payload as any).juror_explain ??
    (result as any).juror_explain ??
    '';

  return {
    type: 'ai_judgment',
    aiWinner: (result.winner as 'num1' | 'num2' | 'tie' | null) ?? null,
    aiVotes: votes
      ? { num1: Number(votes.num1 ?? 0), num2: Number(votes.num2 ?? 0) }
      : null,
    judgmentReason: reason,
    num1Summary: summarize.num1 || '',
    num2Summary: summarize.num2 || '',
  };
}

/** (옵션) 외부에서도 재사용할 수 있게 votes만 안전 추출 */
export function juryVotesOf(payloadOrVotes: any) {
  const v =
    (payloadOrVotes?.result && payloadOrVotes.result.votes) ??
    payloadOrVotes?.votes ??
    null;
  if (!v) return null;
  return {
    num1: Number(v.num1 ?? 0),
    num2: Number(v.num2 ?? 0),
    // 필요시 none: Number(v.none ?? 0)
  };
}

/** 디버깅용: 콘솔에서 구조 확인 */
export function analyzeAIResponse(payload: any) {
  console.group('🔍 /summaries/result 응답 분석');
  console.log('원본 AI 응답:', payload);

  if (isVoteDecided(payload)) {
    const parsed = extractVoteDecidedData(payload);
    console.log('✅ 투표 승부 응답 (요약만)', parsed);
  } else if (isAIJudgment(payload)) {
    const parsed = extractAIJudgmentData(payload);
    console.log('✅ AI 판정 응답 (AI votes = result.votes)', parsed);
  } else {
    console.warn('❌ 알 수 없는 AI 응답 구조:', payload);
  }

  console.groupEnd();
}
