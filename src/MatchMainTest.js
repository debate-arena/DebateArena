// src/MatchMainTest.js
import React, { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const WS_URL = "http://localhost:8081/ws"; // 백엔드의 WebSocket 엔드포인트에 맞게 수정

function MatchMainTest() {
  const [connected, setConnected] = useState(false);
  const [status, setStatus] = useState(null);
  const clientRef = useRef(null);

  useEffect(() => {
    // STOMP 클라이언트 생성
    const socket = new SockJS(WS_URL);
    const client = new Client({
      webSocketFactory: () => socket,
      debug: function (str) {
        console.log(str);
      },
      reconnectDelay: 5000,
    });

    client.onConnect = () => {
      setConnected(true);
      // 매칭 상태 구독
      client.subscribe("/sub/match/status", (message) => {
        console.log("[수신] /sub/match/status:", message.body);
        setStatus(JSON.parse(message.body));
      });
      // /match/main으로 메시지 전송 (서버에서 principal 필요)
      client.publish({ destination: "/match/main" });
      console.log("[전송] /match/main: (payload 없음)");
    };

    client.onDisconnect = () => {
      setConnected(false);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  return (
    <div>
      <h2>매칭 메인 테스트</h2>
      <div>WebSocket 연결 상태: {connected ? "연결됨" : "끊김"}</div>
      <div>
        <h3>매칭 상태</h3>
        <pre>{status ? JSON.stringify(status, null, 2) : "없음"}</pre>
      </div>
    </div>
  );
}

export default MatchMainTest;
