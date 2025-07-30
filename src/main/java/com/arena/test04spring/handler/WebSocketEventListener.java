package com.arena.test04spring.handler;

import com.arena.test04spring.config.RoomManager;
import com.arena.test04spring.config.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final Map<String, String> sessionMap;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomManager roomManager;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        
        // JWT 인증 정보 가져오기 - Authentication 객체와 세션 속성 모두 확인
        String userEmail = null;
        
        // 1. Authentication 객체에서 확인
        if (accessor.getUser() != null) {
            userEmail = accessor.getUser().getName();
            log.debug("1. User {} authenticated for session: {}", userEmail, sessionId);
        }
        
        // 2. 세션 속성에서 확인 (Authentication이 없는 경우)
        if (userEmail == null) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                userEmail = (String) sessionAttributes.get("userEmail");
            }
            log.debug("2. User {} authenticated for session: {}", userEmail, sessionId);
        }
        
        log.info("WebSocket connection established for session: {}({})",
                 sessionId, userEmail != null ? userEmail : "anonymous");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = SimpMessageHeaderAccessor.wrap(event.getMessage()).getSessionId();
        log.info("[disconnected]: {}", sessionId);
        
        try {
            // 세션 정보 제거
            sessionMap.remove(sessionId);
            
            // 방에서 참가자 제거 및 다른 참가자들에게 알림
            cleanupParticipant(sessionId);
            
        } catch (Exception e) {
            log.error("Error during session cleanup for session {}: {}", sessionId, e.getMessage(), e);
        }
    }

    private void cleanupParticipant(String sessionId) {
        // 세션이 속한 방 찾아서 참가자 제거
        Long roomId = roomManager.getRoomBySessionId(sessionId);
        if (roomId != null) {
            roomManager.removeParticipant(roomId, sessionId);
            
            // 같은 방의 다른 참가자들에게 퇴장 알림
            roomManager.getParticipants(roomId).forEach(participantSessionId -> {
                messagingTemplate.convertAndSendToUser(
                    participantSessionId,
                    "/queue/participant-left",
                    Map.of("userEmail", sessionId, "roomId", roomId)
                );
            });
            
            log.info("Participant {} removed from room {}", sessionId, roomId);
        }
    }
}