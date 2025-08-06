import { ref, onBeforeUnmount } from 'vue'

/** 분할 사유(트리거) 정의 */
export type FlushReason =
  | 'punct'   // 문장부호로 끝남 (.?!…/。！？/..이상)
  | 'size'    // 글자수/단어수 제한 초과
  | 'silence' // 침묵 타임아웃
  | 'latency' // 최대 대기(지연) 타임아웃
  | 'end'     // 인식 종료 시 잔여
  | 'error'   // 오류 시 잔여
  | 'manual'  // 수동 호출

/** 완성된 세그먼트 카드 구조 */
export interface Segment {
  id: number;         // 증가하는 시퀀스 ID
  text: string;       // 잘린 텍스트
  reason: FlushReason;// 왜 잘렸는지
  at: number;         // 타임스탬프(ms)
}

/** 설정 값 */
export interface UseSpeechRecognitionOptions {
  maxChars: number;        // 세그먼트 최대 글자수
  maxWords: number;        // 세그먼트 최대 단어수(공백 기준)
  maxLatencyMs: number;    // 새 입력이 없어도 기다려줄 최대 시간
  silenceMs: number;       // "활동 없음"으로 판단할 침묵 시간
  lang: string;            // 인식 언어
  autoStopMs: number | null; // 자동 종료(ms). 끄려면 null
  sentencePunct: RegExp;   // 문장 끝 추정 정규식
}

/** 기본값: 안전한 보통 세팅 */
const DEFAULTS: UseSpeechRecognitionOptions = {
  maxChars: 300,
  maxWords: 50,
  maxLatencyMs: 3500,
  silenceMs: 1200,
  lang: 'ko-KR',
  autoStopMs: 60_000,
  sentencePunct: /[.?!…]|[。！？]|[\.]{2,}$/,
}

/** 유틸: 공백 정규화 및 단어수 계산 */
const normalize = (s: string) => s.replace(/\s+/g, ' ').trim()
const countWords = (s: string) => (s.trim() ? s.trim().split(/\s+/).length : 0)

/**
 * Web Speech API(브라우저)의 타입이 TS에 완전치 않으므로 최소 형태만 선언.
 * 실제 브라우저 객체를 그대로 쓰되, 우리가 접근하는 속성만 타입으로 명시.
 */
type MinimalSpeechRecognition = {
  start(): void
  stop(): void
  lang: string
  continuous: boolean
  interimResults: boolean
  maxAlternatives: number
  onresult: ((e: any) => void) | null
  onerror: ((e: any) => void) | null
  onend: (() => void) | null
}

/** 브라우저에서 생성자 꺼내오기 (지원 확인 포함) */
function createSpeechRecognition(): MinimalSpeechRecognition | null {
  const SR: any = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (!SR) return null
  return new SR()
}

/**
 * 핵심 컴포저블: 음성을 받아 preview/segments를 제공.
 * - preview: "확정 누적 + 중간 결과" 미리보기
 * - segments: 잘린 카드 목록(최신이 맨 앞)
 * - start/stop/flush 메서드 제공
 */
export function useSpeechRecognition(options?: Partial<UseSpeechRecognitionOptions>) {
  // 설정 병합
  const cfg: UseSpeechRecognitionOptions = { ...DEFAULTS, ...(options ?? {}) }

  // ===== 반응형 상태 =====
  const running = ref(false)           // 인식 중 여부
  const preview = ref('')              // 화면 미리보기(확정 누적 + interim)
  const segments = ref<Segment[]>([])  // 잘린 카드
  const lastError = ref<string | null>(null) // 최근 오류 메시지
  const counts = ref({ chars: 0, words: 0 }) // 현재 preview 길이

  // ===== 내부 상태 =====
  let recognition: MinimalSpeechRecognition | null = null
  let buffer = ''              // "확정된" 텍스트 누적 버퍼 (interim 제외)
  let seq = 1                  // 세그먼트 ID 시퀀스
  let autoStopTimer: ReturnType<typeof setTimeout> | undefined
  let latencyTimer: ReturnType<typeof setTimeout> | undefined
  let silenceTimer: ReturnType<typeof setTimeout> | undefined

  // 타이머 유틸
  const clearTimers = () => {
    if (autoStopTimer) clearTimeout(autoStopTimer)
    if (latencyTimer) clearTimeout(latencyTimer)
    if (silenceTimer) clearTimeout(silenceTimer)
    autoStopTimer = latencyTimer = silenceTimer = undefined
  }
  const armLatency = () => {
    if (!cfg.maxLatencyMs) return
    if (latencyTimer) clearTimeout(latencyTimer)
    latencyTimer = setTimeout(() => flush('latency'), cfg.maxLatencyMs)
  }
  const armSilence = () => {
    if (!cfg.silenceMs) return
    if (silenceTimer) clearTimeout(silenceTimer)
    silenceTimer = setTimeout(() => flush('silence'), cfg.silenceMs)
  }
  const armBoth = () => { armLatency(); armSilence() }

  /** 세그먼트 카드 추가 + 미리보기/카운트 리셋 */
  const pushSegment = (text: string, reason: FlushReason) => {
    const t = text.trim()
    if (!t) return
    segments.value = [{ id: seq++, text: t, reason, at: Date.now() }, ...segments.value]
    preview.value = ''
    counts.value = { chars: 0, words: 0 }
  }

  /** 강제 플러시: 현재 buffer를 카드로 만들고 초기화 */
  function flush(reason: FlushReason = 'manual') {
    clearTimeout(latencyTimer!)
    clearTimeout(silenceTimer!)
    const piece = buffer.trim()
    buffer = ''
    if (piece) pushSegment(piece, reason)
    // 인식이 계속 중이면 타이머 재장전
    if (running.value) armBoth()
  }

  /** 최종 결과(확정) 수신 시 버퍼에 합치고, 조건 만족 시 분할 */
  function addFinalChunk(text: string) {
    const clean = normalize(text)
    if (!clean) return

    // 누적
    buffer += (buffer ? ' ' : '') + clean

    // preview는 "현재 확정 누적"을 기준으로
    preview.value = buffer
    counts.value = { chars: preview.value.length, words: countWords(preview.value) }

    // 1) 문장부호로 끝났다면 즉시 분할
    if (cfg.sentencePunct.test(clean.slice(-1))) {
      flush('punct')
      return
    }
    // 2) 크기 제한 초과 시 분할
    if (buffer.length >= cfg.maxChars || countWords(buffer) >= cfg.maxWords) {
      flush('size')
      return
    }
    // 3) 아직 안 잘랐다면 타이머만 리셋
    armBoth()
  }

  /** 시작: Recognition 생성/설정/이벤트 바인딩 */
  function start() {
    if (running.value) return
    recognition = createSpeechRecognition()
    if (!recognition) {
      lastError.value = '이 브라우저는 Web Speech API를 지원하지 않습니다. (Chrome 권장)'
      return
    }

    // 초기화
    segments.value = []
    lastError.value = null
    buffer = ''
    seq = 1
    preview.value = ''
    counts.value = { chars: 0, words: 0 }

    // 옵션 세팅
    recognition.lang = cfg.lang
    recognition.continuous = true
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    // 이벤트
    recognition.onresult = (e: any) => {
      let finalChunk = ''
      let interimChunk = ''
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const r = e.results[i]
        const t: string = r[0].transcript
        if (r.isFinal) finalChunk += t
        else interimChunk += t
      }

      // 미리보기: interim이 있으면 overlay (보여주기 전용)
      if (interimChunk) {
        const overlay = normalize(interimChunk)
        const v = buffer ? `${buffer} ${overlay}` : overlay
        preview.value = v
        counts.value = { chars: v.length, words: countWords(v) }
        // 타이머도 "활동"으로 간주하여 리셋
        armBoth()
      }
      // 실제 분할 판단은 최종 조각에만
      if (finalChunk) addFinalChunk(finalChunk)
    }

    recognition.onerror = (e: any) => {
      lastError.value = `오류: ${e?.error ?? 'unknown'}`
      flush('error') // 남은 꼬리 배출
      stop()         // 데모에선 오류 시 정지
    }

    recognition.onend = () => {
      running.value = false
      clearTimers()
      // 남은 꼬리
      if (buffer.trim()) flush('end')
    }

    // 타이머 가동
    clearTimers()
    if (cfg.autoStopMs) {
      autoStopTimer = setTimeout(() => stop(true), cfg.autoStopMs)
    }
    armBoth()

    recognition.start()
    running.value = true
  }

  /** 정지: 안전하게 stop 호출 */
  function stop(fromAuto = false) {
    if (!running.value) return
    running.value = false
    clearTimers()
    try { recognition?.stop() } catch { /* noop */ }
    if (fromAuto) {
      // 자동 종료 메시지를 lastError에 넣을지 여부는 취향
      // lastError.value = '자동 종료되었습니다.'
    }
  }

  // 뷰 수명주기: 컴포넌트가 파괴되면 안전 정지
  onBeforeUnmount(() => stop(false))

  /** 외부에서 설정을 부분 업데이트하고 싶을 때 */
  function updateConfig(patch: Partial<UseSpeechRecognitionOptions>) {
    Object.assign(cfg, patch)
  }

  return {
    // 상태
    running,
    preview,
    segments,
    counts,
    lastError,

    // 행위
    start,
    stop,
    flush,         // 필요 시 수동 분할
    updateConfig,  // 동적으로 컷 기준 조정

    // debug/참조용
    config: cfg,
  }
} 