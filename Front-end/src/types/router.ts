import type { RtpCapabilities } from "mediasoup-client/types";

export interface Participant {
  userEmail?: string;
  producerUserEmail?: string;
  producerId: string;
  audioStream?: MediaStream | null;
  consumerTransport?: any;
  audioElement?: HTMLAudioElement | null;
  consumer?: any;
  consumerTimeoutId?: number | null;
}

export interface RouterInfo {
  rtpCapabilities: RtpCapabilities;
  participants: Participant[];
}
