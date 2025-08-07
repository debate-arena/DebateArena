<template>
  <div class="p-4 space-y-4 max-w-3xl mx-auto">
    <!-- 연결 상태 & 버튼 -->
    <header class="flex items-center gap-3">
      <h1 class="text-xl font-bold">STT 테스트</h1>
      <span :class="['inline-flex items-center gap-2 px-2 py-0.5 rounded text-sm',
                     isSttConnected ? 'bg-green-100' : 'bg-gray-100']">
        <span :style="{ width:'8px', height:'8px', borderRadius:'9999px',
                        background: isSttConnected ? '#16a34a' : '#9ca3af' }"></span>
        {{ isSttConnected ? '연결됨' : '연결 끊김' }}
      </span>
      <div class="ml-auto flex gap-2">
        <button class="btn" :disabled="isSttConnected" @click="connectStt">연결</button>
        <button class="btn" :disabled="!isSttConnected" @click="disconnectStt">해제</button>
      </div>
    </header>

    <!-- 제어 버튼 -->
    <section class="space-y-2">
      <h2 class="font-semibold">🎛️ 제어</h2>
      <div class="grid grid-cols-3 gap-2">
        <!-- isReady computed 속성을 통해 버튼의 활성화/비활성화 상태를 제어합니다. -->
        <button class="btn" :disabled="!isReady" @click="startOpinion">발언 시작</button>
        <button class="btn" :disabled="!isStopping" @click="stopStt">발언 중지</button>

        <button class="btn" :disabled="!isReady" @click="startAttack">공격 시작</button>
        <button class="btn" :disabled="!isStopping" @click="stopStt">공격 중지</button>

        <button class="btn" :disabled="!isReady" @click="startDefense">방어 시작</button>
        <button class="btn" :disabled="!isStopping" @click="stopStt">방어 중지</button>
      </div>
    </section>

    <!-- 프리뷰 -->
    <section class="space-y-2">
      <h2 class="font-semibold">🎧 프리뷰(내 음성 실시간)</h2>
      <div class="min-h-12 p-3 rounded border bg-white whitespace-pre-wrap">
        {{ previewText || '마이크에 말씀해 보세요...' }}
      </div>
    </section>

    <!-- 수신 로그 -->
    <section class="space-y-2">
      <div class="flex items-center justify-between">
        <h2 class="font-semibold">📨 브로드캐스트 수신 (/user/queue/stt/broadcast)</h2>
        <button class="px-2 py-1 rounded border" @click="clear">로그 비우기</button>
      </div>
      <div class="log-box">
        <div v-if="recv.length===0" class="text-gray-500">수신된 메시지가 없습니다.</div>
        <div v-for="(m,i) in recv" :key="i" class="entry"><pre>{{ fmt(m) }}</pre></div>
      </div>
    </section>

    <!-- 내가 보낸 세그먼트 -->
    <section class="space-y-2">
      <h2 class="font-semibold">✅ 내가 보낸 세그먼트</h2>
      <div class="log-box">
        <div v-if="sent.length===0" class="text-gray-500">전송 내역이 없습니다.</div>
        <div v-for="(m,i) in sent" :key="i" class="entry"><pre>{{ fmt(m) }}</pre></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, watch, watchEffect } from 'vue'; // watch, computed 추가
import { useRoute } from 'vue-router';
import { Client } from '@stomp/stompjs';
import { useStt } from '@/composables/useSpeechRecognition';
import { config } from '@/config/env';

const route = useRoute();
const roomId = computed(() => {
  const id = route.params.id;
  return typeof id === 'string' ? id : Array.isArray(id) ? id[0] : '123';
}); // URL 파라미터에서 roomId 가져오기

// STT용 WebSocket 클라이언트 (8082 포트)
const sttStompClient = ref(null);
const isSttConnected = ref(false);

// STT WebSocket 연결
const connectStt = () => {
  return new Promise((resolve, reject) => {
    const client = new Client({
      brokerURL: `${config.STT_WS_URL}/ws`, // 8082 포트로 연결
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    });

    client.onConnect = () => {
      console.log('🔗 STT WebSocket 연결 성공 (8082)');
      isSttConnected.value = true;
      sttStompClient.value = client;
      resolve();
    };

    client.onStompError = (error) => {
      console.error('❌ STT WebSocket 연결 실패:', error);
      isSttConnected.value = false;
      reject(error);
    };

    client.activate();
  });
};

// STT WebSocket 연결 해제
const disconnectStt = () => {
  if (sttStompClient.value && isSttConnected.value) {
    sttStompClient.value.deactivate();
    isSttConnected.value = false;
    sttStompClient.value = null;
  }
};

// STT WebSocket 연결 시도
connectStt().catch(console.error);

const {
  isRecognizing,
  previewText,
  start,
  stop: stopStt,
  onSegmentReady,
  onSttMessage
} = useStt(sttStompClient, roomId);

// 입장(join) 로직 – 연결 직후 1회
const joined = ref(false);
watchEffect(() => {
  if (isSttConnected.value && !joined.value && roomId.value) {
    sttStompClient.value?.publish({ destination: '/pub/debate/join', body: String(roomId.value) });
    joined.value = true;
  }
});
watch(isSttConnected, (isConnected)=>{ if(!isConnected) joined.value = false; });

// 로그
const recv = ref([]), sent = ref([]);
onSttMessage((p)=> recv.value.unshift({ ts: Date.now(), payload: p }));
onSegmentReady((s)=> sent.value.unshift({ ts: Date.now(), segment: s }));

// 버튼 상태 helpers
// isRecognizing 상태와 동일. STT가 활성화되었을 때 true. 중지 버튼의 활성화 조건입니다.
const isStopping = computed(()=> isRecognizing.value);
// [수정됨] STT를 시작할 준비가 되었는지 확인하는 computed 속성입니다.
// WebSocket이 연결되어 있고, STT가 비활성화 상태일 때만 true가 됩니다.
// 매개변수가 필요 없으므로 computed 속성으로 만들어 가독성과 성능을 개선합니다.
const isReady = computed(()=> !isRecognizing.value && isSttConnected.value);

// 버튼 핸들러
const startOpinion = ()=> start({ phase:'OPINION' });
const startAttack  = ()=> start({ phase:'BATTLE', isAttack:true  });
const startDefense = ()=> start({ phase:'BATTLE', isAttack:false });

const clear = ()=> { recv.value=[]; sent.value=[]; };
const fmt = (o)=> JSON.stringify(o, null, 2);
</script>

<style scoped>
*{box-sizing:border-box}
.btn{padding:.5rem .75rem;border:1px solid #d4d4d8;border-radius:.375rem}
.btn:disabled{opacity:0.5;cursor:not-allowed;}
.log-box{max-height:40vh;overflow:auto;background:#fff;border:1px solid #e5e7eb;border-radius:.375rem;padding:.5rem}
.entry{border:1px solid #e2e8f0;padding:.5rem;border-radius:.375rem;margin-bottom:.5rem;font-size:0.875rem}
</style> 