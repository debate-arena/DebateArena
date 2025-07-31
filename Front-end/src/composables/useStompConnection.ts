import { ref, onUnmounted } from 'vue';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from '@/store/auth';

export function useStompConnection() {
  const client = ref<Client | null>(null);
  const isConnected = ref(false);
  const authStore = useAuthStore();
  
  const connect = (brokerURL: string, authToken?: string, testEmail?: string) => {
    // 테스트 모드 확인
    const isTestMode = import.meta.env.VITE_TEST_MODE === 'true' || import.meta.env.NODE_ENV === 'development'
    const userEmail = testEmail || (isTestMode ? import.meta.env.VITE_TEST_USER_EMAIL || 'test@example.com' : authStore.userEmail)
    
    client.value = new Client({
      brokerURL: brokerURL,
      connectHeaders: {
        // OAuth2 쿠키 기반 인증을 사용하므로 토큰은 선택적
        ...(authToken && { 'Authorization': `Bearer ${authToken}` }),
        'login': userEmail
      },
      debug: function (str) {
        console.log('STOMP Debug:', str)
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: (frame) => {
        isConnected.value = true;
        console.log('✅ STOMP 연결됨:', frame);
      },
      onDisconnect: (frame) => {
        isConnected.value = false;
        console.log('❌ STOMP 연결 해제됨:', frame);
      },
      onStompError: (frame) => {
        console.error('💥 STOMP 오류:', frame);
        isConnected.value = false;
      },
      onWebSocketError: (error) => {
        console.error('🔌 WebSocket 오류:', error);
        isConnected.value = false;
      },
      onWebSocketClose: (event) => {
        console.log('🔌 WebSocket 연결 종료:', event);
        isConnected.value = false;
      }
    });
    
    client.value.activate();
  };
  
  const disconnect = () => {
    if (client.value && client.value.connected) {
      client.value.deactivate();
    }
  };

  // 메시지 구독
  const subscribe = (destination: string, callback: (message: any) => void) => {
    if (client.value && client.value.connected) {
      return client.value.subscribe(destination, callback);
    }
    console.warn('STOMP 클라이언트가 연결되지 않음');
    return null;
  };

  // 메시지 발송
  const publish = (destination: string, body: any, headers: Record<string, string> = {}) => {
    if (client.value && client.value.connected) {
      client.value.publish({
        destination,
        body: JSON.stringify(body),
        headers: {
          'content-type': 'application/json',
          ...headers
        }
      });
    } else {
      console.warn('STOMP 클라이언트가 연결되지 않음');
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
    subscribe,
    publish,
  };
}