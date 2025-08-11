<template>
  <div class="mx-auto max-w-4xl p-4 space-y-6">
    <!-- Header -->
    <header class="flex items-center justify-between">
      <h1 class="text-lg font-semibold">STT Demo (Only STT)</h1>
      <span
        class="text-xs rounded-full px-2 py-0.5"
        :class="connected ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'"
      >
        {{ connected ? 'CONNECTED' : 'DISCONNECTED' }}
      </span>
    </header>

    <!-- Connection: 자동 연결 (표시는 헤더 배지로만) -->

    <!-- Room / Config -->
    <section class="grid gap-3 md:grid-cols-4">
      <div>
        <label class="text-sm font-medium">Room ID</label>
        <input v-model="roomId" class="w-full rounded border px-3 py-2" />
      </div>
      <div>
        <label class="text-sm font-medium">Language</label>
        <select v-model="lang" class="w-full rounded border px-3 py-2">
          <option value="ko-KR">ko-KR</option>
          <option value="en-US">en-US</option>
          <option value="ja-JP">ja-JP</option>
        </select>
      </div>
      <div class="md:col-span-2 text-xs text-gray-500 flex items-center">
        * 언어 변경은 새로 시작(Start) 시 반영됩니다.
      </div>
    </section>

    <!-- Controls -->
    <section class="flex flex-wrap gap-2">
      <button class="rounded bg-blue-600 text-white px-3 py-2 disabled:opacity-50"
              :disabled="!connected || isRec"
              @click="startOpinion">🎤 Start (Opinion)</button>

      <button class="rounded bg-indigo-600 text-white px-3 py-2 disabled:opacity-50"
              :disabled="!connected || isRec"
              @click="startBattleAttack">⚔️ Start (Battle · Attack)</button>

      <button class="rounded bg-purple-600 text-white px-3 py-2 disabled:opacity-50"
              :disabled="!connected || isRec"
              @click="startBattleDefense">🛡 Start (Battle · Defense)</button>

      <button class="rounded border px-3 py-2 disabled:opacity-50"
              :disabled="!isRec"
              @click="stop">⏹ Stop</button>
    </section>

    <!-- Preview / Received -->
    <section class="grid gap-4 md:grid-cols-2">
      <div class="space-y-2">
        <h2 class="font-semibold">Interim Preview</h2>
        <div class="rounded border p-3 min-h-12 text-gray-700">
          {{ stt.previewText || '...' }}
        </div>
        <p class="text-xs text-gray-500">브라우저 Web Speech API의 interim 결과</p>
      </div>

      <div class="space-y-2">
        <h2 class="font-semibold">Last 10 STT Broadcasts</h2>
        <div class="rounded border p-0 overflow-hidden">
          <table class="w-full text-sm">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-3 py-2 text-left">Time</th>
                <th class="px-3 py-2 text-left">User</th>
                <th class="px-3 py-2 text-left">Text</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in lastStt" :key="m.key" class="odd:bg-white even:bg-gray-50">
                <td class="px-3 py-2">{{ m.time }}</td>
                <td class="px-3 py-2">{{ m.user }}</td>
                <td class="px-3 py-2 break-words">{{ m.text }}</td>
              </tr>
              <tr v-if="!lastStt.length">
                <td colspan="3" class="px-3 py-6 text-center text-gray-500">No messages</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="text-xs text-gray-500">수신 토픽은 컴포저블에서 자동 구독합니다.</p>
      </div>
    </section>

    <!-- Room preview (RoomStore) -->
    <section class="space-y-2">
      <h2 class="font-semibold">Room Preview (RoomStore)</h2>
      <div class="rounded border p-3 text-sm whitespace-pre-wrap">
        {{ roomPreview }}
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Client } from '@stomp/stompjs'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useStt } from '@/composables/useSpeechRecognition'
import { config } from '@/config/env'
import { useRoomStore } from '@/store/roomStore'

// --- connection (기본 8082 환경변수 사용) ---
const defaultWsUrl = `${config.STT_WS_URL}/ws`
const wsUrl = ref(defaultWsUrl)
const client = ref<Client | null>(null)
const connected = ref(false)

function connect() {
  if (client.value) client.value.deactivate()
  const c = new Client({
    brokerURL: wsUrl.value,
    reconnectDelay: 3000,
    onConnect: () => { connected.value = true },
    onWebSocketClose: () => { connected.value = false },
    onStompError: (f) => { console.error('[STOMP ERROR]', f) },
  })
  client.value = c
  c.activate()
}
function disconnect() {
  client.value?.deactivate()
  connected.value = false
}

// --- room/config ---
const roomId = ref<string | number>('123')
const lang = ref<'ko-KR' | 'en-US' | 'ja-JP'>('ko-KR')

// --- useStt (현재 컴포저블 시그니처에 맞춤) ---
const stt = useStt(client, roomId, {
  lang: lang.value,
  // 나머지 세그먼테이션 설정이 필요하면 여기서 추가 가능
})

// 방송 수신 로그
const sttFeed = ref<{ key: string; time: string; user: string; text: string }[]>([])

// ApiResponse<BroadcastResponse> 또는 BroadcastResponse 둘 다 대비
stt.onSttMessage((payload: any) => {
  const body = payload?.data?.text ? payload.data : payload
  const user = body?.user ?? '-'
  const text = body?.text ?? JSON.stringify(payload)
  const now = new Date()
  sttFeed.value.unshift({
    key: `${now.getTime()}_${Math.random()}`,
    time: now.toLocaleTimeString(),
    user,
    text,
  })
  if (sttFeed.value.length > 200) sttFeed.value.pop()
})

// controls
function startOpinion()      { stt.startOpinion() }
function startBattleAttack() { stt.startBattleAttack() }
function startBattleDefense(){ stt.startBattleDefense() }
function stop()              { stt.stop() }

const lastStt = computed(() => sttFeed.value.slice(0, 10))

// refs for boolean props to satisfy TS Booleanish
const isRec = computed(() => !!stt.isRecognizing.value)

// router에서 roomId 수신, 마운트 시 자동 연결
const route = useRoute()
const routeRoomId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? id : Array.isArray(id) ? id[0] : ''
})

// RoomStore preview
const roomStore = useRoomStore()
const roomPreview = computed(() => {
  const r = roomStore.room
  if (!r) return 'No room in store'
  const left = r.participants.filter(p => p.side === 'L').map(p => p.displayName)
  const right = r.participants.filter(p => p.side === 'R').map(p => p.displayName)
  return JSON.stringify({ roomId: r.roomId, leftTeam: left, rightTeam: right }, null, 2)
})

onMounted(() => {
  if (routeRoomId.value) {
    roomId.value = routeRoomId.value
  }
  connect()
})
onUnmounted(() => { disconnect() })
</script>

<style scoped>
.min-h-12 { min-height: 3rem; }
</style>