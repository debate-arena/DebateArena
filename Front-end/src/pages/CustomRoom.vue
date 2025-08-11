<template>
  <div class="max-w-4xl mx-auto p-6 space-y-6">
    <h1 class="text-2xl font-bold">커스텀 토론 방</h1>

    <!-- 생성 폼: 호스트 전용 (방이 없을 때) -->
    <Card v-if="!customRoom.state.roomId" class="p-6">
      <div class="grid grid-cols-1 gap-6">
        <div class="space-y-2">
          <Label for="topic">주제</Label>
          <Input
            id="topic"
            v-model="form.topic"
            placeholder="토론 주제를 입력하세요"
          />
        </div>
        <div class="space-y-3">
          <Label for="mode">모드</Label>
          <div class="flex gap-6">
            <label class="flex items-center space-x-2">
              <input
                type="radio"
                v-model="form.mode"
                value="1:1"
                class="w-4 h-4"
              />
              <span>1 대 1</span>
            </label>
            <label class="flex items-center space-x-2">
              <input
                type="radio"
                v-model="form.mode"
                value="2:2"
                class="w-4 h-4"
              />
              <span>2 대 2</span>
            </label>
          </div>
        </div>
        <div class="space-y-2">
          <Label for="left">1팀</Label>
          <Input id="left" v-model="form.leftTeamName" placeholder="예: 찬성" />
        </div>
        <div class="space-y-2">
          <Label for="right">2팀</Label>
          <Input
            id="right"
            v-model="form.rightTeamName"
            placeholder="예: 반대"
          />
        </div>
      </div>
      <div class="flex justify-end gap-2">
        <Button
          :disabled="!canCreate"
          :class="{
            'opacity-50 cursor-not-allowed': !canCreate,
            'hover:bg-primary/90 hover:scale-105 transition-all duration-200 border-1 border-primary':
              canCreate,
          }"
          @click="handleCreate"
        >
          방 생성
        </Button>
      </div>
    </Card>

    <!-- 로비: 방이 있을 때 -->
    <Card v-else class="p-6 space-y-4">
      <div class="flex items-start justify-between mb-0">
        <div>
          <div class="text-sm text-muted-foreground">방 ID</div>
          <div class="font-mono text-sm">{{ customRoom.state.roomId }}</div>
        </div>
        <div class="text-right">
          <div class="text-xs">인원: {{ customRoom.requiredPerSide }} x 2</div>
          <div class="text-xs">
            호스트: {{ customRoom.state.ownerNickname }}
          </div>
        </div>
      </div>

      <div class="space-y-2">
        <div
          class="text-4xl font-bold break-words whitespace-pre-line leading-tight max-w-full text-center mb-6"
        >
          {{ customRoom.state.topic || "주제 미정" }}
        </div>
        <div class="grid grid-cols-12 gap-2 items-center justify-center">
          <div class="text-xl font-bold text-center col-span-5">1팀</div>
          <div class="col-span-2"></div>
          <div class="text-xl font-bold text-center col-span-5">2팀</div>
          <div class="text-sm text-muted-foreground flex items-center font-bold col-span-5 px-4">
            {{ customRoom.state.leftTeamName }}
          </div>
          <div class="col-span-2"></div>
          <div class="text-sm text-muted-foreground flex items-center font-bold col-span-5 px-4">
            {{ customRoom.state.rightTeamName }}
          </div>
        </div>
      </div>

      <!-- 팀 선택 영역 -->
      <div class="grid grid-cols-12 gap-4 items-start">
        <Card class="p-3 space-y-2 col-span-5">
          <div class="font-semibold flex items-center justify-center relative">
            <Badge variant="outline" class="text-xs absolute right-0 top-0">
              {{ customRoom.leftParticipants.length }}/{{
                customRoom.requiredPerSide
              }}
            </Badge>
          </div>
          <div class="space-y-1">
            <!-- 실제 참가자들 -->
            <div
              v-for="p in customRoom.leftParticipants"
              :key="p.email"
              class="text-xs flex justify-between h-[2.6em] items-center"
            >
              <span
                class="break-words whitespace-pre-line leading-tight max-w-full"
                >{{ p.nickname.replace(/(.{8})/g, "$1\n") }}</span
              >
              <Badge :variant="p.ready ? 'default' : 'secondary'">{{
                p.ready ? "준비완료" : "대기"
              }}</Badge>
            </div>
            <!-- 빈자리 표시 -->
            <div
              v-for="n in customRoom.requiredPerSide -
              customRoom.leftParticipants.length"
              :key="`empty-left-${n}`"
              class="text-xs flex justify-between h-[2.6em] items-center opacity-40"
            >
              <span class="text-muted-foreground">빈자리</span>
              <Badge class="text-muted-foreground">대기</Badge>
            </div>
          </div>
          <Button
            size="sm"
            variant="secondary"
            :disabled="isInSide('L') || isSideFull('L') || meReady"
            @click="joinSide('L')"
            class="hover:bg-primary/90 hover:scale-105 transition-all duration-200 border-1 border-primary"
            >참가</Button
          >
        </Card>

        <div class="flex items-center justify-center h-full col-span-2">
          <div class="text-2xl font-bold">VS</div>
        </div>

        <Card class="p-3 space-y-2 col-span-5">
          <div class="font-semibold flex items-center justify-center relative">
            <Badge variant="outline" class="text-xs absolute right-0 top-0">
              {{ customRoom.rightParticipants.length }}/{{
                customRoom.requiredPerSide
              }}
            </Badge>
          </div>
          <div class="space-y-1">
            <!-- 실제 참가자들 -->
            <div
              v-for="p in customRoom.rightParticipants"
              :key="p.email"
              class="text-xs flex justify-between h-[2.6em] items-center"
            >
              <span
                class="break-words whitespace-pre-line leading-tight max-w-full"
                >{{ p.nickname.replace(/(.{8})/g, "$1\n") }}</span
              >
              <Badge :variant="p.ready ? 'default' : 'secondary'">{{
                p.ready ? "준비완료" : "대기"
              }}</Badge>
            </div>
            <!-- 빈자리 표시 -->
            <div
              v-for="n in customRoom.requiredPerSide -
              customRoom.rightParticipants.length"
              :key="`empty-right-${n}`"
              class="text-xs flex justify-between h-[2.6em] items-center opacity-40"
            >
              <span class="text-muted-foreground">빈자리</span>
              <Badge class="text-muted-foreground">대기</Badge>
            </div>
          </div>
          <Button
            size="sm"
            variant="secondary"
            :disabled="isInSide('R') || isSideFull('R') || meReady"
            @click="joinSide('R')"
            class="hover:bg-primary/90 hover:scale-105 transition-all duration-200 border-1 border-primary"
            >참가</Button
          >
        </Card>
      </div>

      <!-- 대기 인원 -->
      <Card class="p-3">
        <div class="font-semibold mb-2">대기 중</div>
        <div class="flex flex-wrap gap-2">
          <Badge
            v-for="p in customRoom.waitingParticipants"
            :key="p.email"
            variant="outline"
            >{{ p.nickname }}</Badge
          >
          <div
            v-if="customRoom.waitingParticipants.length === 0"
            class="text-sm text-muted-foreground"
          >
            없음
          </div>
        </div>
      </Card>

      <!-- 내 컨트롤 -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Badge v-if="meSide" variant="secondary"
            >내 진영:
            {{
              meSide === "L"
                ? customRoom.state.leftTeamName
                : customRoom.state.rightTeamName
            }}</Badge
          >
        </div>
        <div class="flex items-center gap-2">
          <Button
            variant="outline"
            @click="toggleReady()"
            :disabled="!meSide"
            >{{ meReady ? "준비 해제" : "준비" }}</Button
          >
          <Button
            variant="secondary"
            @click="backToWaiting()"
            :disabled="!meSide && !meReady"
            class="hover:bg-primary/90 hover:scale-105 transition-all duration-200 border-1 border-primary"
            >대기중으로</Button
          >
          <Button
            v-if="customRoom.isOwner"
            :disabled="!customRoom.allReady"
            @click="startDebate"
            >시작</Button
          >
        </div>
      </div>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/store/auth";
import { useRoomStore } from "@/store/roomStore";
import { useCustomRoomStore, type CustomMode } from "@/store/customRoom";
import { useDebateStore } from "@/store/useDebateStore";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const roomStore = useRoomStore();
const customRoom = useCustomRoomStore();
const debateStore = useDebateStore();

const form = reactive<{
  topic: string;
  leftTeamName: string;
  rightTeamName: string;
  mode: CustomMode;
}>({
  topic: "",
  leftTeamName: "",
  rightTeamName: "",
  mode: "1:1",
});

const canCreate = computed(
  () =>
    form.topic.trim().length > 0 && !!form.leftTeamName && !!form.rightTeamName
);

function handleCreate() {
  const roomId = customRoom.createRoom({
    topic: form.topic.trim(),
    leftTeamName: form.leftTeamName.trim(),
    rightTeamName: form.rightTeamName.trim(),
    mode: form.mode,
  });
  // 공유를 위해 쿼리로 roomId 전달 (실서비스는 초대 링크나 서버 생성 응답 사용)
  router.replace({ name: route.name || "CustomRoom", query: { id: roomId } });
}

// 초대 링크로 진입했을 때 자동 합류
const qRoomId = route.query.id as string | undefined;
if (qRoomId) {
  customRoom.joinRoom(qRoomId);
}

const me = computed(() =>
  customRoom.state.participants.find(
    (p) => p.email === (auth.userEmail || "guest@example.com")
  )
);
const meSide = computed(() => me.value?.side || null);
const meReady = computed(() => !!me.value?.ready);

const isSideFull = (side: "L" | "R") =>
  (side === "L"
    ? customRoom.leftParticipants.length
    : customRoom.rightParticipants.length) >= customRoom.requiredPerSide;
const isInSide = (side: "L" | "R") => meSide.value === side;

function joinSide(side: "L" | "R") {
  const email = auth.userEmail || "guest@example.com";
  customRoom.selectSide(email, side);
}

function toggleReady() {
  const email = auth.userEmail || "guest@example.com";
  customRoom.toggleReady(email);
}

function backToWaiting() {
  const email = auth.userEmail || "guest@example.com";
  customRoom.backToWaiting(email);
}

function startDebate() {
  if (!customRoom.allReady) return;
  const display = customRoom.toRoomDisplay();
  if (!display) return;
  // DebateRoom에서 사용할 표시용 데이터 주입
  roomStore.setRoom({
    roomId: display.roomId,
    participants: display.participants,
    topic: customRoom.state.topic,
    leftTeamName: customRoom.state.leftTeamName,
    rightTeamName: customRoom.state.rightTeamName,
  });
  // 내 정보 설정 (진영/호스트 여부)
  const myEmail = auth.userEmail || "guest@example.com";
  const me = customRoom.state.participants.find((p) => p.email === myEmail);
  debateStore.setRoomId(display.roomId);
  debateStore.setMyInfo(myEmail, (me?.side as any) || null, customRoom.isOwner);
  // DebateRoom으로 이동
  router.push({ name: "Debate", params: { id: display.roomId } });
}
</script>

<style scoped></style>
