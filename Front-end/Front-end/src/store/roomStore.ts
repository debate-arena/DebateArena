import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { RoomDisplay } from '@/types/room';
import type { ParticipantDisplay } from '@/types/participant';

export const useRoomStore = defineStore('room', () => {
  /* ---------- state ---------- */
  const room = ref<RoomDisplay | null>(null);

  /* ---------- getters ---------- */
  /** 좌·우 진영 멤버 분리 */
  const leftTeam = computed(() =>
    room.value ? room.value.participants.filter(p => p.side === 'L') : [],
  );
  const rightTeam = computed(() =>
    room.value ? room.value.participants.filter(p => p.side === 'R') : [],
  );
  const isReady = computed(() => !!room.value && room.value.participants.length > 0);

  /* ---------- actions ---------- */
  function setRoom(data: RoomDisplay) {
    room.value = data;
  }

  function clearRoom() {
    room.value = null;
  }

  function updateParticipant(participant: ParticipantDisplay) {
    if (!room.value) return;
    const idx = room.value.participants.findIndex(p => p.userId === participant.userId);
    if (idx === -1) room.value.participants.push(participant);
    else room.value.participants[idx] = participant;
  }

  return {
    /* state */
    room,
    /* getters */
    leftTeam,
    rightTeam,
    isReady,
    /* actions */
    setRoom,
    clearRoom,
    updateParticipant,
  };
});
