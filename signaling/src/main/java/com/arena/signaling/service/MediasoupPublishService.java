package com.arena.signaling.service;

import com.arena.signaling.dto.request.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediasoupPublishService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RoomManageService roomManager;

    public void createRouter(SimpMessageHeaderAccessor headerAccessor, CreateRouterRequest req) {
        // RoomId를 세션에 저장
        setAttribute(headerAccessor, "roomId", req.getRoomId());

        req.setUserEmail(getAttribute(headerAccessor, "userEmail", String.class));

        roomManager.addRoom(req);

        redisTemplate.convertAndSend("mediasoup:router:create", req);
        log.info("[라우터 생성 요청] roomId: {}, userEmail: {}", req.getRoomId(), req.getUserEmail());
    }

    public void createTransport(SimpMessageHeaderAccessor headerAccessor, CreatedTransportRequestDto req) {
        req.setUserEmail(getAttribute(headerAccessor, "userEmail", String.class));
        req.setRoomId(getAttribute(headerAccessor, "roomId", Long.class));
        req.setSessionId(getSessionId(headerAccessor));

        redisTemplate.convertAndSend("mediasoup:transport:create", req);
        log.info("[Transport 생성 요청] roomId: {}, userEmail: {}", req.getRoomId(), req.getUserEmail());
    }


    public void connectTransport(SimpMessageHeaderAccessor headerAccessor, ConnectTransportRequestDto req) {
        req.setUserEmail(getAttribute(headerAccessor, "userEmail", String.class));
        req.setRoomId(getAttribute(headerAccessor, "roomId", Long.class));

        redisTemplate.convertAndSend("mediasoup:transport:connect", req);
        log.info("[Transport 연결 요청] roomId: {}, userEmail: {}", req.getRoomId(), req.getUserEmail());
    }

    public void createProducer(SimpMessageHeaderAccessor headerAccessor, CreateProducerRequestDto req) {
        req.setUserEmail(getAttribute(headerAccessor, "userEmail", String.class));
        req.setRoomId(getAttribute(headerAccessor, "roomId", Long.class));

        redisTemplate.convertAndSend("mediasoup:producer:create", req);
        log.info("[Producer 생성 요청] roomId: {}, userEmail: {}", req.getRoomId(), req.getUserEmail());
    }

    public void createConsumer(SimpMessageHeaderAccessor headerAccessor, CreateConsumerRequestDto req) {
        req.setUserEmail(getAttribute(headerAccessor, "userEmail", String.class));
        req.setRoomId(getAttribute(headerAccessor, "roomId", Long.class));

        redisTemplate.convertAndSend("mediasoup:consumer:create", req);
        log.info("[Consumer 생성 요청] roomId: {}, userEmail: {}", req.getRoomId(), req.getUserEmail());
    }

    public void resume(SimpMessageHeaderAccessor headerAccessor, JsonNode messageNode){
        String consumerId = messageNode.get("consumerId").asText();

        Map<String, Object> request = new HashMap<>();
        request.put("consumerId", consumerId);

        redisTemplate.convertAndSend("mediasoup:consumer:resume", request);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAttribute(SimpMessageHeaderAccessor accessor, String key, Class<T> type) {
        if (accessor == null) {
            throw new IllegalArgumentException("세션이 존재하지 않습니다.");
        }
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new IllegalStateException("["+ accessor +"] 속성이 존재하지 않습니다.");
        }
         Object value = sessionAttributes.get(key);
        if (value == null) {
            throw new IllegalStateException("[" + key + "] 해당 속성이 없습니다.");
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException("[" + key + "] 해당 속성의 변환 타입이 맞지 않습니다. " + type.getName());
        }
        return (T) value;
    }

    public static <T> void setAttribute(SimpMessageHeaderAccessor accessor, String key, T value) {
        if (accessor == null) {
            throw new IllegalArgumentException("세션이 존재하지 않습니다.");
        }
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new IllegalStateException("세션 속성이 생성되지 않았습니다.");
        }
        sessionAttributes.put(key, value);
    }

    public static String getSessionId(SimpMessageHeaderAccessor accessor) {
        if (accessor == null) {
            throw new IllegalArgumentException("세션이 존재하지 않습니다.");
        }
        String sessionId = accessor.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalStateException("세션 ID가 존재하지 않거나 비어 있습니다.");
        }

        return sessionId;
    }
}