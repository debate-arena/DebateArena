import { ref, watch, onUnmounted, unref } from 'vue';

// Vue의 Ref 객체이거나 일반 객체일 수 있는 값을 안전하게 추출하는 헬퍼 함수.
const getClient = (refOrObj: any) => refOrObj?.value ?? refOrObj;

/**
 * 실시간 음성-텍스트 변환(STT) 및 WebSocket(STOMP) 통신을 관리하는 Vue 3 Composable(훅).
 *
 * 이 훅은 STT 세그먼테이션, Web Speech API 제어, STOMP 통신, 상태 관리 등
 * 복잡한 로직을 캡슐화하여 컴포넌트에서 쉽게 사용할 수 있도록 제공합니다.
 * 하나의 인스턴스를 생성 후, `start()` 메서드를 통해 모드를 바꿔가며 재사용할 수 있습니다.
 *
 * @param {import('vue').Ref<import('@stomp/stompjs').Client> | import('@stomp/stompjs').Client} stompClientRef - STOMP 클라이언트 인스턴스.
 * @param {string|number|import('vue').Ref<string|number>} roomId - STOMP 토픽 경로를 구성하는 데 사용될 방 번호.
 * @param {object} [config] - STT 동작을 미세 조정하기 위한 선택적 설정 객체.
 */
export function useStt(stompClientRef: any, roomId: any, config = {}) {
  /* ──────────────── 기본 설정 ─────────────── */
  const cfg = {
    lang: 'ko-KR',
    maxChars: 300,
    maxWords: 50,
    maxLatencyMs: 3500,
    silenceMs: 1200,
    sentencePunct: /[.?!…]|[。！？]|[\.]{2,}$/,
    autoSend: true,
    ...config,
  };

  /* ──────────────── 반응형 상태 ─────────────── */
  const isRecognizing = ref(false);
  const previewText   = ref('');
  const buffer        = ref('');
  let recognition: any = null;

  /* ──────────────── 현재 전송 모드 ─────────────── */
  let phase    = 'OPINION';
  let isAttack = false;
  let seq      = 0;

  /* ──────────────── 사용자 콜백 슬롯 ─────────────── */
  // 훅 외부(컴포넌트)에서 특정 이벤트에 대한 로직을 주입할 수 있도록 콜백 함수 슬롯을 마련합니다.
  let onSegReady = (segment: any) => {}; // 세그먼트 생성 시 호출될 콜백
  let onSttMsg   = (message: any) => {}; // STOMP 메시지 수신 시 호출될 콜백
  let onSystem   = (signal: any)  => {}; // (확장용) 시스템 신호 수신 시 호출될 콜백

  /* ──────────────── STT 세그먼테이션 로직 ─────────────── */
  let latencyT: any = null, silenceT: any = null;
  const clearTimers = () => { clearTimeout(latencyT); clearTimeout(silenceT); };
  const scheduleTimers = () => {
    clearTimers();
    if (cfg.maxLatencyMs) latencyT = setTimeout(() => flush('latency'), cfg.maxLatencyMs);
    if (cfg.silenceMs)   silenceT = setTimeout(() => flush('silence'), cfg.silenceMs);
  };
  const wordCnt = (s: string) => s.trim() ? s.trim().split(/\s+/).length : 0;

  const flush = (reason = 'manual') => {
    const txt = buffer.value.trim();
    if (!txt) { previewText.value = ''; clearTimers(); return; }

    const segment = { seq: ++seq, text: txt, reason, ts: Date.now() };
    try { onSegReady(segment); } catch(e) { console.error("onSegReady callback error:", e); }

    if (cfg.autoSend) publish(segment);

    buffer.value = ''; previewText.value = ''; clearTimers();
  };

  const handleFinal = (finalTxt: string) => {
    const needsSpace = buffer.value && !buffer.value.endsWith(' ');
    buffer.value += (needsSpace ? ' ' : '') + finalTxt.trim();
    scheduleTimers();

    const now = buffer.value;
    if (cfg.sentencePunct.test(now.slice(-2))) return flush('punct');
    if (now.length >= cfg.maxChars)           return flush('chars');
    if (wordCnt(now) >= cfg.maxWords)         return flush('words');
  };

  /* ──────────────── Web Speech API ─────────────── */
  const SR = (typeof window !== 'undefined') && ((window as any).SpeechRecognition || (window as any).webkitSpeechRecognition);
  const createRec = () => {
    const rec = new SR();
    rec.lang = cfg.lang; rec.interimResults = true; rec.continuous = true;

    rec.onresult = (e: any) => {
      const last = Array.from(e.results).pop();
      if (!last) return;

      if (!(last as any).isFinal) {
        previewText.value = (last as any)[0]?.transcript ?? '';
        scheduleTimers();
      } else {
        previewText.value = '';
        handleFinal((last as any)[0]?.transcript ?? '');
      }
    };

    rec.onend = () => { if (isRecognizing.value) rec.start(); };
    rec.onerror = (e: any) => {
      const ignorable = ['no-speech', 'aborted', 'audio-capture'];
      if (!ignorable.includes(e?.error)) console.error('[STT] Recognition Error:', e);
    };
    return rec;
  };

  /* ──────────────── STOMP publish/subscribe ─────────────── */
  const publish = (segment: any) => {
    const client = getClient(stompClientRef);
    const rid    = unref(roomId);
    if (!client?.connected || !rid) return;

    const dest = phase === 'OPINION'
      ? `/pub/debate/${rid}/stt/opinion`
      : `/pub/debate/${rid}/stt/battle`;

    const body = phase === 'OPINION'
      ? { text: segment.text, idx: segment.seq }
      : { text: segment.text, idx: segment.seq, isAttack };

    client.publish({ destination: dest, body: JSON.stringify(body) });
  };

  let subs: any[] = [];
  const subPaths = () => {
    const rid = unref(roomId);
    return rid ? {
      broadcast: `/user/queue/stt/broadcast`,
      system:    `/sub/debate/${rid}/system`, // (확장용)
    } : null;
  };

  const subscribe = () => {
    const paths = subPaths(); const client = getClient(stompClientRef);
    if (!client?.connected || !paths) return;
    unsubscribe();

    subs.push(
      client.subscribe(paths.broadcast, (message: any) => {
        try { onSttMsg(JSON.parse(message.body)); } catch(e) { console.error("Error parsing STT message:", e); }
      })
    );
    // (예시) 시스템 토픽 구독 로직
    // subs.push(
    //   client.subscribe(paths.system, (m) => { try { onSystem(JSON.parse(m.body)); } catch {} })
    // );
  };
  const unsubscribe = () => { subs.forEach(s => s?.unsubscribe()); subs = []; };

  watch(() => getClient(stompClientRef)?.connected, (ok) => ok ? subscribe() : unsubscribe(), { immediate: true });
  watch(() => unref(roomId), () => {
    if (getClient(stompClientRef)?.connected) {
      subscribe();
    }
  });
  onUnmounted(() => { unsubscribe(); if (isRecognizing.value) stop(); });

  /* ──────────────── start / stop (공개 API) ─────────────── */
  const start = ({ phase: ph = 'OPINION', isAttack: atk = false } = {}) => {
    if (!SR || isRecognizing.value) return;
    phase = ph; isAttack = atk;

    recognition = createRec();
    recognition.start();
    isRecognizing.value = true;
  };

  const stop = () => {
    if (!isRecognizing.value) return;
    isRecognizing.value = false;
    try { recognition?.stop(); } catch {}
    recognition = null;
    flush('stop');
  };

  /* ──────────────── API 노출 ─────────────── */
  return {
    isRecognizing,
    previewText,
    start,
    stop,
    startOpinion: () => start({ phase: 'OPINION' }),
    startBattleAttack: () => start({ phase: 'BATTLE', isAttack: true }),
    startBattleDefense: () => start({ phase: 'BATTLE', isAttack: false }),
    onSegmentReady: (cb: any) => { onSegReady = cb || onSegReady; },
    onSttMessage:  (cb: any) => { onSttMsg   = cb || onSttMsg;   },
    onSystemSignal:(cb: any) => { onSystem   = cb || onSystem;   },
  };
} 