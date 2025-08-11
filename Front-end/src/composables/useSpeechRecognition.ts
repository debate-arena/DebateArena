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
export interface OpinionSummaryResponse { result?: { text?: string; [k: string]: any } }
export interface SiegeDefenseResponse { result?: { text?: string; [k: string]: any } }
export interface DebateResultResponse { result?: { winner?: 'num1' | 'num2' | string; scores?: any; summary?: any; [k: string]: any } }

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
    maxChars: 300,
    maxWords: 50,
    maxLatencyMs: 3500,
    silenceMs: 1200,
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
  let latencyT: any = null,
    silenceT: any = null
  const clearTimers = () => {
    clearTimeout(latencyT)
    clearTimeout(silenceT)
  }
  const scheduleTimers = () => {
    clearTimers()
    if (cfg.maxLatencyMs) latencyT = setTimeout(() => flush('latency'), cfg.maxLatencyMs)
    if (cfg.silenceMs) silenceT = setTimeout(() => flush('silence'), cfg.silenceMs)
  }
  const wordCnt = (s: string) => (s.trim() ? s.trim().split(/\s+/).length : 0)

  const flush = (reason = 'manual') => {
    const txt = buffer.value.trim()
    if (!txt) {
      previewText.value = ''
      clearTimers()
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
    clearTimers()
  }

  const handleFinal = (finalTxt: string) => {
    const needsSpace = buffer.value && !buffer.value.endsWith(' ')
    buffer.value += (needsSpace ? ' ' : '') + finalTxt.trim()
    try { console.info('[STT][SEG][final]', finalTxt) } catch {}
    scheduleTimers()

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
        scheduleTimers()
      } else {
        previewText.value = ''
        handleFinal(last[0]?.transcript ?? '')
      }
    }

    rec.onend = () => {
      if (isRecognizing.value) rec.start()
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
        `/debate/room/${rid}/stt`, // fallback for current AS-IS without /sub
      ],
      // AI summaries (room-wide)
      opinion: [
        `/sub/debate/room/${rid}/summaries/opinion`,
        `/sub/debate/room/${rid}/summaries/opinion${rid}`, // fallback for current bug
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
            try { console.info('[STT][RECV][summary:opinion]', dest, m.body) } catch {}
            onOpinionSummary(JSON.parse(m.body))
          } catch {}
        })
      )
    }
    for (const p of paths.battle) {
      subs.push(
        client.subscribe(p, (m: any) => {
          try {
            const dest = (m as any)?.headers?.destination
            try { console.info('[STT][RECV][summary:battle]', dest, m.body) } catch {}
            onBattleSummary(JSON.parse(m.body))
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
  }
}