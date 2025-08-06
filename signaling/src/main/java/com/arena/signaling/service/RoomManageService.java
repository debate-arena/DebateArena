package com.arena.signaling.service;

import com.arena.signaling.dto.*;
import com.arena.signaling.dto.response.CreatedConsumerDto;
import com.arena.signaling.dto.response.CreatedProducerDto;
import com.arena.signaling.dto.response.CreatedTransportDto;
import com.arena.signaling.model.Participant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomManageService {

    private final Map<String, String> userEmailToSessionId = new ConcurrentHashMap<>();
    private final Map<Long, List<Participant>> rooms = new ConcurrentHashMap<>();
    private final Map<String, Long> userEmailToRoom = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    public boolean isUserInRoom(String userEmail) {
        return userEmailToRoom.containsKey(userEmail);
    }

    // 유저 생성 및
    public void addParticipant(CreatedTransportDto createdTransportDto) {
        List<Participant> participants = rooms.computeIfAbsent(createdTransportDto.getRoomId()
                , k -> new CopyOnWriteArrayList<>());

        Participant participant = participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(createdTransportDto.getUserEmail()))
                .findFirst()
                .orElse(null);

        if (participant == null) {
            participant = Participant.builder()
                    .producerUserEmail(createdTransportDto.getUserEmail())
                    .transportIds(new CopyOnWriteArrayList<>())
                    .build();
            participant.getTransportIds().add(createdTransportDto.getTransportId());
            participants.add(participant);
            log.debug("[새 참가자 입장] email : {} room : {}", createdTransportDto.getUserEmail(), createdTransportDto.getRoomId());
        } else {
            throw new RuntimeException("[참가자 추가 오류] 중복 접속 감지");
//            participant.getTransportIds().add(createdTransportDto.getTransportId());
//            log.debug("[addParticipant] {} is already added to room {}", createdTransportDto.getUserEmail(), createdTransportDto.getRoomId());
        }

        userEmailToSessionId.put(createdTransportDto.getUserEmail(), createdTransportDto.getSessionId());
        userEmailToRoom.put(createdTransportDto.getUserEmail(), createdTransportDto.getRoomId());
        log.debug("[Added participant {} to room {}]", createdTransportDto.getUserEmail(), createdTransportDto.getRoomId());
    }


    public List<Participant> getParticipants(Long roomId) {
        List<Participant> participants = rooms.get(roomId);

        if(participants == null){
            return null;
        }

        List<Participant> filtered = participants.stream()
                .filter(p -> p.getProducerId() != null)
                .collect(Collectors.toList());

        return filtered.isEmpty() ? null : filtered;
    }

    public void removeParticipant(String sessionId, String userEmail) throws JsonProcessingException {
        if(!userEmailToSessionId.get(userEmail).equals(sessionId)){
            log.debug("[참가자 제거] 해당 유저를 세션에서 찾을 수 없습니다 (중복 접속 감지) userEmail: {} ", sessionId);
            return;
        }

        userEmailToSessionId.remove(userEmail);
        Long roomId = userEmailToRoom.get(userEmail);
        if(roomId == null){
            log.debug("removeParticipant - roomId not found Error");
            return;
        }
        List<Participant> participants = rooms.get(roomId);
        if(participants == null){
            log.debug("removeParticipant - room in participants not found Error");
            return;
        }

        userEmailToRoom.remove(userEmail);

        Participant removedParticipant = participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(userEmail))
                .findFirst()
                .orElse(null);

        if(removedParticipant == null){
            log.debug("removeParticipant - userEmail not found Error");
            return;
        }

        participants.removeIf(p -> p.getProducerUserEmail().equals(userEmail));

        // 방에있는 다른 참가자들에게 상태 업데이트
        participants.forEach(participant -> {
            Map<String, Boolean> map = participant.getConsumerConnectedStatus();
            if (map != null && map.containsKey(userEmail) && map.get(userEmail) == Boolean.TRUE) {
                map.put(removedParticipant.getProducerUserEmail(), false);
            }
            Map<String, String> consumerIdMap = participant.getConsumerIdMap();
            if (consumerIdMap != null) {
                consumerIdMap.remove(userEmail);
            }
        });

        if(removedParticipant.getConsumerIdMap() != null && !removedParticipant.getConsumerIdMap().isEmpty()){
            log.debug("[removeParticipant] consumerIdMap is not null");
            removedParticipant.getConsumerIdMap().forEach((consumerEmail, consumerId) -> {
                Map<String, Object> request = new HashMap<>();
                request.put("roomId", roomId);
                request.put("consumerId", consumerId);
                request.put("type", "consumer" );

                String requestMessage = null;
                try {
                    requestMessage = objectMapper.writeValueAsString(request);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                log.debug("[removeParticipant] hasConsumer: {} {} {}", requestMessage,consumerEmail,consumerId);
                redisTemplate.convertAndSend("mediasoup:transport:disconnect", requestMessage);
            });
        }
        if(removedParticipant.getProducerId() != null){
            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("producerId", removedParticipant.getProducerId());
            request.put("type", "producer" );

            String requestMessage = objectMapper.writeValueAsString(request);
            redisTemplate.convertAndSend("mediasoup:transport:disconnect", requestMessage);
            log.debug("[removeParticipant] producer : {}", requestMessage);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("roomId", roomId);
        request.put("transportId", removedParticipant.getTransportIds());
        request.put("type", "produceTransport" );
        String requestMessage = objectMapper.writeValueAsString(request);
        log.debug("[removeParticipant] transport: {}", requestMessage);
        redisTemplate.convertAndSend("mediasoup:transport:disconnect", requestMessage);

        // 같은 방의 다른 참가자들에게 퇴장 알림
        // TODO : 같은방 참가자들에게 퇴장 알림 또는 pending 처리
//        getParticipants(roomId).forEach(participantSessionId -> {
//            messagingTemplate.convertAndSendToUser(
//                    participantSessionId,
//                    "/queue/participant-left",
//                    Map.of("userEmail", userEmail, "roomId", roomId)
//            );
//        });
        // 방이 비어있으면 제거
        if (participants.isEmpty()) {
            log.info("[ removed ] ROOM {} ", roomId);
            rooms.remove(roomId);
        }

        log.info("[ removed ] {} from room {} ", userEmail, roomId);

        participants.forEach(participant -> {
            System.out.println("=== Participant ===");
            System.out.println("Producer Email: " + participant.getProducerUserEmail());

            Map<String, Boolean> statusMap = participant.getConsumerConnectedStatus();
            if (statusMap != null && !statusMap.isEmpty()) {
                statusMap.forEach((consumerEmail, status) ->
                        System.out.println("  Consumer: " + consumerEmail + " | Connected: " + status)
                );
            } else {
                System.out.println("  No consumer status available.");
            }
        });
    }

    public long updateParticipantConnectionInfo(ClientConnectionEstablishedDto clientConnectionEstablishedDto) {
        List<Participant> participants = rooms.get(clientConnectionEstablishedDto.getRoomId());
        Participant participant = participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(clientConnectionEstablishedDto.getConsumerUserEmail()))
                .findFirst().orElse(null);
        if (participant == null) return -1;

        if (clientConnectionEstablishedDto.getIsProducer()) {
            participant.setIsProducerConnected(true);
        }else {
            if (participant.getConsumerConnectedStatus() == null)
                participant.setConsumerConnectedStatus(new HashMap<>());
            participant.getConsumerConnectedStatus().put(clientConnectionEstablishedDto.getProducerUserEmail(), true);
        }

        return participants.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsProducerConnected()))
                .map(Participant::getConsumerConnectedStatus)
                .filter(Objects::nonNull)
                .flatMap(map -> map.values().stream())
                .filter(Boolean::booleanValue)
                .count();
    }

    public void updateParticipantProducerInfo(CreatedProducerDto createdProducerDto) {

        log.info("[updateParticipantProducerInfo] {} ", createdProducerDto);

        List<Participant> participants = rooms.get(createdProducerDto.getRoomId());

        Participant participant = participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(createdProducerDto.getUserEmail()))
                .findFirst()
                .orElse(null);
        if(participant == null){
            return;
        }
        if(participant.getProducerId() == null){
            participant.setProducerId(createdProducerDto.getProducerId());
        }else{
            log.info("[updateParticipantProducerInfo] producerId is not null");
        }

    }

    public void updateParticipantConsumerInfo(CreatedConsumerDto createdConsumerDto) {


        List<Participant> participants = rooms.get(createdConsumerDto.getRoomId());
        log.info("[updateParticipantConsumerInfo] {} ", participants);
        Participant participant = participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(createdConsumerDto.getUserEmail()))
                .findFirst()
                .orElse(null);
        if(participant == null){
            log.info("[updateParticipantConsumerInfo] participant not found");
            return;
        }

        if(participant.getConsumerIdMap()==null){
            participant.setConsumerIdMap(new HashMap<>());
        }

        participant.getConsumerIdMap().put(createdConsumerDto.getProducerUserEmail(), createdConsumerDto.getConsumerId());
        log.info("[updateParticipantConsumerInfo] {} ", participant.getConsumerIdMap());

    }

    public void micOn(MediaControlDto mediaControlDto) {
        log.info("[micOn] {} ", mediaControlDto);
        List<Participant> participants =rooms.get(mediaControlDto.getRoomId());

        participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(mediaControlDto.getSpeaker()))
                .findFirst()
                .ifPresent(participant ->
                        redisTemplate.convertAndSend("mediasoup:producer:mic:on", participant.getProducerId()));
    }
}