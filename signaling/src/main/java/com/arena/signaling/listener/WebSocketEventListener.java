package com.arena.signaling.listener;

import com.arena.signaling.service.RoomManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RoomManageService roomManageService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();
        String userEmail;

        if (accessor.getUser() != null) {
            userEmail = accessor.getUser().getName();
        }else{
            throw new IllegalArgumentException("[connect]인증 정보가 없습니다. 웹 소켓 연결을 해제합니다.");
        }
        
        log.info("[ ws connected ]: {} ({})",sessionId, userEmail);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();
        String userEmail;

        if (accessor.getUser() != null) {
            userEmail = accessor.getUser().getName();
        }else{
            throw new IllegalArgumentException("[disconnect]인증 정보가 없습니다. 웹 소켓 연결을 해제합니다.");
        }

        log.info("[disconnected]: {} ({})", sessionId,userEmail);
        
        try {
            roomManageService.removeParticipant(sessionId,userEmail);
        } catch (Exception e) {
            log.error("Error during session cleanup for session {}: {}", sessionId, e.getMessage(), e);
        }
    }


}