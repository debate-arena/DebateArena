package com.arena.test04spring.service;

import com.arena.test04spring.dto.*;
import com.arena.test04spring.model.Room;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MediasoupPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public MediasoupPublisher(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void createRouter(SimpMessageHeaderAccessor headerAccessor, JsonNode messageNode) throws JsonProcessingException {
        String userEmail = headerAccessor.getSessionAttributes().get("userEmail").toString();
        Long roomId = messageNode.get("roomId").asLong();
        headerAccessor.getSessionAttributes().put("roomId", roomId);
        log.info("[createRouter] roomId: {}, userEmail: {}", roomId, userEmail);

        Map<String, Object> request = new HashMap<>();
        request.put("roomId", roomId);
        request.put("matchType", 1);    // 하드코딩
        request.put("userEmail", userEmail);

        String requestMessage = objectMapper.writeValueAsString(request);
        redisTemplate.convertAndSend("mediasoup:router:create", requestMessage);

    }

    public void createTransport(SimpMessageHeaderAccessor headerAccessor, JsonNode messageNode) throws JsonProcessingException {
        String userEmail = headerAccessor.getSessionAttributes().get("userEmail").toString();
        Long roomId = (Long) headerAccessor.getSessionAttributes().get("roomId");

        boolean isProducer = Boolean.parseBoolean(messageNode.get("isProducer").asText());
        log.debug(" [creatingTransport] {} ", userEmail);

        Map<String, Object> request = new HashMap<>();
        request.put("roomId",roomId);
        request.put("userEmail", userEmail);
        request.put("isProducer", isProducer);
        if(!isProducer){
            request.put("producerUserEmail", messageNode.get("producerUserEmail").asText());
        }

        String requestMessage = objectMapper.writeValueAsString(request);
        redisTemplate.convertAndSend("mediasoup:transport:create", requestMessage);
    }


    public void connectTransport(SimpMessageHeaderAccessor headerAccessor, JsonNode messageNode) throws JsonProcessingException {
        String userEmail = headerAccessor.getSessionAttributes().get("userEmail").toString();
        Long roomId = (Long) headerAccessor.getSessionAttributes().get("roomId");

        String transportId = messageNode.get("transportId").asText();
        JsonNode dtlsParameters = messageNode.get("dtlsParameters");

        Map<String, Object> request = new HashMap<>();
        request.put("userEmail", userEmail);
        request.put("transportId", transportId);
        request.put("dtlsParameters", dtlsParameters);

        String requestMessage = objectMapper.writeValueAsString(request);
        redisTemplate.convertAndSend("mediasoup:transport:connect", requestMessage);
    }

    public void createProducer(SimpMessageHeaderAccessor headerAccessor, JsonNode messageNode) throws JsonProcessingException {
        String userEmail = headerAccessor.getSessionAttributes().get("userEmail").toString();
        Long roomId = (Long) headerAccessor.getSessionAttributes().get("roomId");

        String transportId = messageNode.get("transportId").asText();
        JsonNode rtpParameters = messageNode.get("rtpParameters");
        String kind = messageNode.get("kind").asText();

        Map<String, Object> request = new HashMap<>();
        request.put("userEmail", userEmail);
        request.put("transportId", transportId);
        request.put("kind", kind);
        request.put("rtpParameters", rtpParameters);
        request.put("roomId", roomId);

        String requestMessage = objectMapper.writeValueAsString(request);
        redisTemplate.convertAndSend("mediasoup:producer:create", requestMessage);
    }

    public void createConsumer(SimpMessageHeaderAccessor headerAccessor, JsonNode messageNode) throws JsonProcessingException {
        String userEmail = headerAccessor.getSessionAttributes().get("userEmail").toString();
        Long roomId = (Long) headerAccessor.getSessionAttributes().get("roomId");

        String producerId = messageNode.get("producerId").asText();
        JsonNode rtpCapabilities = messageNode.get("rtpCapabilities");
        String transportId = messageNode.get("transportId").asText();
        String producerUserEmail = messageNode.get("producerUserEmail").asText();

        Map<String, Object> request = new HashMap<>();
        request.put("userEmail", userEmail);
        request.put("producerId", producerId);
        request.put("rtpCapabilities", rtpCapabilities);
        request.put("transportId", transportId);
        request.put("producerUserEmail", producerUserEmail);

        String requestMessage = objectMapper.writeValueAsString(request);
        log.info("[createConsumer] {} ",requestMessage);
        redisTemplate.convertAndSend("mediasoup:consumer:create", requestMessage);
    }


    public void resume(SimpMessageHeaderAccessor headerAccessor, JsonNode messageNode) throws JsonProcessingException {
        String consumerId = messageNode.get("consumerId").asText();

        Map<String, Object> request = new HashMap<>();
        request.put("consumerId", consumerId);

        String requestMessage = objectMapper.writeValueAsString(request);
        redisTemplate.convertAndSend("mediasoup:consumer:resume", requestMessage);
    }
}