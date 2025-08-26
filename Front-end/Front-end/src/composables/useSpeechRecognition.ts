import { ref, watch, onUnmounted, unref } from 'vue'

/**
 * Revised STT composable aligned with current backend:
 * - Publish DTOs: STTRequest / OpinionSTTRequest -> { text }
 * - Broadcast (room-wide): /sub/debate/room/{roomId}/stt (fallback to no /sub)
 * - AI summaries (room-wide):
 *    /sub/debate/room/{roomId}/summaries/opinion (and buggy fallback: +{roomId})
 *    /sub/debate/room/{roomId}/summaries/battle
 *    /sub/debate/room/{roomId}/summaries/result
 * - Spectator personal queues are handled elsewhere (not subscribed here)
 */

// Helper: unwrap ref or plain object
const getClient = (refOrObj: any) => refOrObj?.value ?? refOrObj

// DTO & Types (aligned with backend)
export interface ApiResponse<T> { status?: string; data?: T; [k: string]: any }
export interface BroadcastResponse { user: string; text: string }
export interface OpinionSummaryResponse { result?: { user_id?: string; text?: string; [k: string]: any } }
export interface SiegeDefenseResponse {
  result?: {
    attack_id?: string;      // 공격자 ID
    defense_id?: string;     // 방어자 ID
    text?: string;           // 공방 요약 텍스트
    rebuttal_score?: number; // 반박 점수
    [k: string]: any
  }
}

// 투표에서 승부 결정 → AI가 요약만 제공
export interface VoteDecidedResponse {
  full_summarize: {
    num1: string;    // 승리팀 종합 요약
    num2: string;    // 패배팀 종합 요약
  }
}

// 투표에서 무승부 → AI가 승부 판정
export interface AIJudgmentResponse {
  result: {
    winner: string;                           // "num1" 또는 "num2"
    votes: { num1: number; num2: number };    // 무승부였던 투표 결과
    soft_scores: { num1: number; num2: number };
    details: {
      juror: number;
      vote: string;
      sim1: number;
      sim2: number;
      diff: number;
    };
    juror_explain: string;                    // AI 판정 근거 설명
    full_summarize: {
      num1: string;
      num2: string;
    };
  }
}

// 통합 타입 - /summaries/result에서 받을 수 있는 모든 형태
export type DebateResultResponse = VoteDecidedResponse | AIJudgmentResponse;

export type SttPhase = 'OPINION' | 'BATTLE'

export interface UseSttConfig {
  lang?: string
  maxChars?: number
  maxWords?: number
  maxLatencyMs?: number
  silenceMs?: number
  sentencePunct?: RegExp
  autoSend?: boolean
}

export function useStt(
  stompClientRef: any,
  roomId: string | number | any,
  config: UseSttConfig = {}
) {
  /* ──────────────── config ─────────────── */
  const cfg: Required<UseSttConfig> = {
    lang: 'ko-KR',
    maxChars: 60,
    maxWords: 12,
    maxLatencyMs: 300,      // 계속 말해도 이 주기로 강제 flush (하드-라티ency)
    silenceMs: 350,         // 멈추면 이 타이밍에 flush
    sentencePunct: /[.?!…]|[。！？]|[\.]{2,}$/,
    autoSend: true,
    ...config,
  }

  /* ──────────────── reactive state ─────────────── */
  const isRecognizing = ref(false)
  const previewText = ref('')
  const buffer = ref('')
  let recognition: any = null

  /* ──────────────── mode ─────────────── */
  let phase: SttPhase = 'OPINION'
  let seq = 0

  /* ──────────────── callbacks (user slots) ─────────────── */
  let onSegReady = (_segment: any) => {}
  let onSttMsg = (_payload: ApiResponse<BroadcastResponse> | BroadcastResponse) => {}
  let onOpinionSummary = (_payload: OpinionSummaryResponse) => {}
  let onBattleSummary = (_payload: SiegeDefenseResponse) => {}
  let onResultSummary = (_payload: DebateResultResponse) => {}
  let onSystem = (_signal: any) => {}

  /* ──────────────── segmentation ─────────────── */
  // ⬇️ 하드-라티ency: 한번 무장되면 interim 이벤트로 리셋되지 않음
  let latencyT: any = null
  let silenceT: any = null
  let latencyDeadlineAt: number | null = null

  const nowTs = () => (typeof performance !== 'undefined' ? performance.now() : Date.now())

  const clearAllTimers = () => {
    clearTimeout(latencyT); latencyT = null
    clearTimeout(silenceT); silenceT = null
    latencyDeadlineAt = null
  }

  // 침묵 타이머: 이벤트마다 리셋 (말 멈춤 감지)
  const armSilence = () => {
    clearTimeout(silenceT)
    if (cfg.silenceMs) {
      silenceT = setTimeout(() => flush('silence'), cfg.silenceMs)
    }
  }

  // 지연 타이머: 텍스트가 처음 생긴 시점에만 1회 무장 (하드-라티ency)
  const armLatencyIfNeeded = () => {
    if (!cfg.maxLatencyMs) return
    if (latencyDeadlineAt != null) return       // 이미 무장됨
    const now = nowTs()
    latencyDeadlineAt = now + cfg.maxLatencyMs
    latencyT = setTimeout(() => flush('latency'), cfg.maxLatencyMs)
    try { console.debug('[STT][TIMER] arm latency', cfg.maxLatencyMs) } catch {}
  }

  const wordCnt = (s: string) => (s.trim() ? s.trim().split(/\s+/).length : 0)

  const flush = (reason = 'manual') => {
    const txt = buffer.value.trim()
    if (!txt) {
      previewText.value = ''
      clearAllTimers()
      return
    }

    const segment = { seq: ++seq, text: txt, reason, ts: Date.now() }
    try {
      try { console.info('[STT][SEG][flush]', reason, segment) } catch {}
      onSegReady(segment)
    } catch (e) {
      console.error('[STT] onSegReady error:', e)
    }

    if (cfg.autoSend) publish(segment)

    buffer.value = ''
    previewText.value = ''
    clearAllTimers()
  }

  const handleFinal = (finalTxt: string) => {
    const needsSpace = buffer.value && !buffer.value.endsWith(' ')
    buffer.value += (needsSpace ? ' ' : '') + finalTxt.trim()
    try { console.info('[STT][SEG][final]', finalTxt) } catch {}

    // 하드-라티ency & 침묵 타이머 운용
    if (buffer.value) armLatencyIfNeeded()
    armSilence()

    const now = buffer.value
    if (cfg.sentencePunct.test(now.slice(-2))) return flush('punct')
    if (now.length >= cfg.maxChars) return flush('chars')
    if (wordCnt(now) >= cfg.maxWords) return flush('words')
  }

  /* ──────────────── Web Speech API ─────────────── */
  const SR =
    typeof window !== 'undefined' &&
    ((window as any).SpeechRecognition || (window as any).webkitSpeechRecognition)

  const createRec = () => {
    const rec = new SR()
    rec.lang = cfg.lang
    rec.interimResults = true
    rec.continuous = true

    rec.onresult = (e: any) => {
      const last = Array.from(e.results).pop() as any
      if (!last) return

      if (!last.isFinal) {
        previewText.value = last[0]?.transcript ?? ''
        try { console.debug('[STT][PREVIEW]', previewText.value) } catch {}
        // ⬇️ interim에도 침묵은 리셋, 지연은 "필요 시 최초만" 무장
        if (previewText.value || buffer.value) armLatencyIfNeeded()
        armSilence()
      } else {
        previewText.value = ''
        handleFinal(last[0]?.transcript ?? '')
      }
    }

    rec.onend = () => {
      // 일부 브라우저에서 연속 start 제약 완화
      if (isRecognizing.value) setTimeout(() => rec.start(), 150)
    }

    rec.onerror = (e: any) => {
      const ignorable = ['no-speech', 'aborted', 'audio-capture']
      if (!ignorable.includes(e?.error)) console.error('[STT] Recognition Error:', e)
    }
    return rec
  }

  /* ──────────────── STOMP publish ─────────────── */
  const publish = (segment: any) => {
    const client = getClient(stompClientRef)
    const rid = unref(roomId)
    if (!client?.connected || !rid) return

    const dest =
      phase === 'OPINION'
        ? `/pub/debate/${rid}/stt/opinion`
        : `/pub/debate/${rid}/stt/battle`

    // IMPORTANT: DTO is STTRequest/OpinionSTTRequest => { text } only
    const body = { text: segment.text }

    try { console.info('[STT][SEND]', dest, body) } catch {}
    client.publish({ destination: dest, body: JSON.stringify(body) })
  }

  /* ──────────────── STOMP subscribe ─────────────── */
  let subs: any[] = []
  let isSubscribed = false

  const subPaths = () => {
    const rid = unref(roomId)
    if (!rid) return null
    return {
      // STT broadcast (room-wide)
      stt: [
        `/sub/debate/room/${rid}/stt`, // recommended, once backend is fixed
        `/debate/room/${rid}/stt`,     // fallback for current AS-IS without /sub
      ],
      // AI summaries (room-wide)
      opinion: [
        `/sub/debate/room/${rid}/summaries/opinion`,
        `/sub/debate/room/${rid}/summaries/opinion${rid}`, // fallback for current bug (문자열 그대로 유지)
      ],
      battle: [`/sub/debate/room/${rid}/summaries/battle`],
      result: [`/sub/debate/room/${rid}/summaries/result`],
      // (optional) system channel example
      system: [`/sub/debate/${rid}/system`],
    }
  }

  const subscribe = () => {
    const paths = subPaths()
    const client = getClient(stompClientRef)
    if (!client?.connected || !paths) return
    unsubscribe()

    // STT broadcast messages
    for (const p of paths.stt) {
      subs.push(
        client.subscribe(p, (message: any) => {
          try {
            const dest = (message as any)?.headers?.destination
            try { console.info('[STT][RECV][stt]', dest, message.body) } catch {}
            onSttMsg(JSON.parse(message.body))
          } catch (e) {
            console.error('[STT] parse error:', e)
          }
        })
      )
    }

    // AI summaries
    for (const p of paths.opinion) {
      subs.push(
        client.subscribe(p, (m: any) => {
          try {
            const dest = (m as any)?.headers?.destination
            const parsed = JSON.parse(m.body)
            try { console.info('[STT][RECV][summary:opinion]', dest, 'RAW:', m.body, 'PARSED:', parsed) } catch {}
            onOpinionSummary(parsed)
          } catch {}
        })
      )
    }
    for (const p of paths.battle) {
      subs.push(
        client.subscribe(p, (m: any) => {
          try {
            const dest = (m as any)?.headers?.destination
            const parsed = JSON.parse(m.body)
            try { console.info('[STT][RECV][summary:battle]', dest, 'RAW:', m.body, 'PARSED:', parsed) } catch {}
            onBattleSummary(parsed)
          } catch {}
        })
      )
    }
    for (const p of paths.result) {
      subs.push(
        client.subscribe(p, (m: any) => {
          try {
            const dest = (m as any)?.headers?.destination
            try { console.info('[STT][RECV][summary:result]', dest, m.body) } catch {}
            onResultSummary(JSON.parse(m.body))
          } catch {}
        })
      )
    }

    // Optional system channel
    for (const p of paths.system) {
      subs.push(
        client.subscribe(p, (m: any) => {
          try {
            const dest = (m as any)?.headers?.destination
            try { console.info('[STT][RECV][system]', dest, m.body) } catch {}
            onSystem(JSON.parse(m.body))
          } catch {}
        })
      )
    }
    isSubscribed = true
    try { console.info('[STT] Subscribed to topics:', paths) } catch {}
  }

  const unsubscribe = () => {
    subs.forEach((s) => s?.unsubscribe?.())
    subs = []
    isSubscribed = false
  }

  // 연결 상태 폴링 기반 감지 (stomp Client의 connected는 비반응 속성)
  const connectionFlag = ref<boolean>(false)
  let connectionTimer: any = null

  const startConnectionWatcher = () => {
    if (connectionTimer) return
    connectionTimer = setInterval(() => {
      const ok = !!getClient(stompClientRef)?.connected
      if (ok !== connectionFlag.value) {
        connectionFlag.value = ok
      }
      if (ok && !isSubscribed) {
        subscribe()
      }
    }, 500)
  }

  const stopConnectionWatcher = () => {
    if (connectionTimer) {
      clearInterval(connectionTimer)
      connectionTimer = null
    }
  }

  watch(connectionFlag, (ok) => {
    if (ok) subscribe()
    else unsubscribe()
  }, { immediate: true })

  watch(
    () => unref(roomId),
    () => {
      if (getClient(stompClientRef)?.connected) subscribe()
    }
  )

  onUnmounted(() => {
    unsubscribe()
    if (isRecognizing.value) stop()
    stopConnectionWatcher()
  })

  // 시작 시 폴링 워처 구동
  startConnectionWatcher()

  /* ──────────────── public API ─────────────── */
  const start = (args: { phase?: SttPhase; isAttack?: boolean } = {}) => {
    if (!SR) {
      console.warn('[STT] Web Speech API not supported in this browser/context')
      return
    }
    if (isRecognizing.value) {
      console.debug('[STT] Already recognizing')
      return
    }
    const { phase: ph = 'OPINION' as SttPhase } = args
    phase = ph

    recognition = createRec()
    recognition.start()
    isRecognizing.value = true
    console.info('[STT] Recognition started. Phase:', phase)
  }

  const stop = () => {
    if (!isRecognizing.value) return
    isRecognizing.value = false
    try {
      recognition?.stop()
    } catch {}
    recognition = null
    flush('stop')
    console.info('[STT] Recognition stopped')
  }

  const setPhase = (ph: SttPhase) => {
    phase = ph
  }

  return {
    // state
    isRecognizing,
    previewText,

    // controls
    start,
    stop,
    flushNow: () => flush('manual'),
    setPhase,
    startOpinion: () => start({ phase: 'OPINION' }),
    startBattleAttack: () => start({ phase: 'BATTLE', isAttack: true }),
    startBattleDefense: () => start({ phase: 'BATTLE', isAttack: false }),

    // callback registrations
    onSegmentReady: (cb: any) => {
      onSegReady = cb || onSegReady
    },
    onSttMessage: (cb: any) => {
      onSttMsg = cb || onSttMsg
    },
    onOpinionSummary: (cb: any) => {
      onOpinionSummary = cb || onOpinionSummary
    },
    onBattleSummary: (cb: any) => {
      onBattleSummary = cb || onBattleSummary
    },
    onResultSummary: (cb: any) => {
      onResultSummary = cb || onResultSummary
    },
    onSystemSignal: (cb: any) => {
      onSystem = cb || onSystem
    },

    // debug
    subscribe: () => subscribe(),
    unsubscribe: () => unsubscribe(),
  }
}
