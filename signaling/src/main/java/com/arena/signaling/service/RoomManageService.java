package com.arena.signaling.service;

import com.arena.signaling.dto.*;
import com.arena.signaling.dto.request.CreateRouterRequest;
import com.arena.signaling.dto.response.CreatedConsumerDto;
import com.arena.signaling.dto.response.CreatedProducerDto;
import com.arena.signaling.model.Participant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomManageService {

    private final Map<Long, List<Participant>> rooms = new ConcurrentHashMap<>();
    private final Map<String, Long> userEmailToRoom = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    public boolean isUserInRoom(String userEmail) {

        return userEmailToRoom.containsKey(userEmail);
    }

    public void addRoom(CreateRouterRequest req) {
        List<Participant> participants = rooms.computeIfAbsent(req.getRoomId()
                , k -> new CopyOnWriteArrayList<>());
        userEmailToRoom.put(req.getUserEmail(), req.getRoomId());
    }

    // 유저 생성 및
    public void addParticipant(CreatedProducerDto createdProducerDto) {
        List<Participant> participants = rooms.computeIfAbsent(createdProducerDto.getRoomId()
                , k -> new CopyOnWriteArrayList<>());

        Participant participant = participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(createdProducerDto.getUserEmail()))
                .findFirst()
                .orElse(null);

        if (participant == null) {
            participant = Participant.builder()
                    .producerUserEmail(createdProducerDto.getUserEmail())
                    .producerId(createdProducerDto.getProducerId())
                    .build();
            participants.add(participant);
            log.debug("[새 참가자 입장] email : {} room : {}", createdProducerDto.getUserEmail(), createdProducerDto.getRoomId());
        } else {
            log.debug("[중복 요청 감지] email : {} room : {}", createdProducerDto.getUserEmail(), createdProducerDto.getRoomId());
        }

        userEmailToRoom.put(createdProducerDto.getUserEmail(), createdProducerDto.getRoomId());
        log.debug("[참가자 추가 (add producer) {} to room {}]", createdProducerDto.getUserEmail(), createdProducerDto.getRoomId());
    }


    public List<Participant> getParticipants(Long roomId) {
        List<Participant> participants = rooms.get(roomId);

        if(participants == null){
            return null;
        }

        List<Participant> filtered = new CopyOnWriteArrayList<>(participants);
        return filtered.isEmpty() ? null : filtered;
    }

    public void removeParticipant(String userEmail) throws JsonProcessingException {
        // Redis 에 삭제 요청
        if(userEmail == null){
            return;
        }
        Long roomId = userEmailToRoom.get(userEmail);
        if(roomId == null){
            log.debug("removeParticipant - roomId not found Error");
            return;
        }

        Map<String, Object> map2 = new HashMap<>();
        map2.put("roomId", roomId);
        map2.put("userEmail", userEmail);
        String jsonString = objectMapper.writeValueAsString(map2);
        redisTemplate.convertAndSend("mediasoup:transport:disconnect", jsonString);


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

        // 방에있는 다른 참가자들의 해당 유저와의 연결 상태 업데이트
        participants.forEach(participant -> {
            Map<String, Boolean> map = participant.getConsumerConnectedStatus();
            if (map != null && map.containsKey(userEmail) && map.get(userEmail) == Boolean.TRUE) {
                map.remove(removedParticipant.getProducerUserEmail());
            }
        });


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

        if (clientConnectionEstablishedDto == null) return -1;

        Long roomId = clientConnectionEstablishedDto.getRoomId();
        if (roomId == null || !rooms.containsKey(roomId)) return -1;

        List<Participant> participants = rooms.get(roomId);
        if (participants == null) return -1;

        String consumerEmail = clientConnectionEstablishedDto.getConsumerUserEmail();
        if (consumerEmail == null) return -1;

        Participant participant = participants.stream()
                .filter(p -> consumerEmail.equals(p.getProducerUserEmail()))
                .findFirst()
                .orElse(null);

        if (participant == null) return -1;

        if (clientConnectionEstablishedDto.getIsProducer()) {
            participant.setIsProducerConnected(true);
            return -2;
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

//    public void updateParticipantProducerInfo(CreatedProducerDto createdProducerDto) {
//
//        log.info("[updateParticipantProducerInfo] {} ", createdProducerDto);
//
//        List<Participant> participants = rooms.get(createdProducerDto.getRoomId());
//
//        Participant participant = participants.stream()
//                .filter(p -> p.getProducerUserEmail().equals(createdProducerDto.getUserEmail()))
//                .findFirst()
//                .orElse(null);
//        if(participant == null){
//            return;
//        }
//        if(participant.getProducerId() == null){
//            participant.setProducerId(createdProducerDto.getProducerId());
//        }else{
//            log.info("[updateParticipantProducerInfo] producerId is not null");
//        }
//
//    }
//
     //
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

        if(participant.getConsumerConnectedStatus()==null){
            participant.setConsumerConnectedStatus(new HashMap<>());
        }

        participant.getConsumerConnectedStatus().put(createdConsumerDto.getProducerUserEmail(), true);
        log.info("[updateParticipantConsumerInfo] {} ", participant.getConsumerConnectedStatus());

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