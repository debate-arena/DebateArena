import { ref, onUnmounted } from 'vue';
import { Client } from '@stomp/stompjs';

export function useStompConnection() {
  const client = ref<Client | null>(null);
  const isConnected = ref(false);
  
  const connect = (url: string) => {
    client.value = new Client({
      brokerURL: url,
      onConnect: () => {
        isConnected.value = true;
        console.log('STOMP 연결됨');
      },
      onDisconnect: () => {
        isConnected.value = false;
        console.log('STOMP 연결 해제됨');
      },
      onStompError: (frame) => {
        console.error('STOMP 오류:', frame);
      },
    });
    
    client.value.activate();
  };
  
  const disconnect = () => {
    if (client.value) {
      client.value.deactivate();
    }
  };
  
  onUnmounted(() => {
    disconnect();
  });
  
  return {
    client,
    isConnected,
    connect,
    disconnect,
  };
}