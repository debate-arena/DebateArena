import type { ParticipantDisplay } from '@/types/participant';

export interface RoomDisplay {
  roomId: string;
  participants: ParticipantDisplay[];
}