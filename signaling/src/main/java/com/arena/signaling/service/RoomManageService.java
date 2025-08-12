package com.arena.signaling.service;

import com.arena.signaling.dto.*;
import com.arena.signaling.dto.request.CreateRouterRequest;
import com.arena.signaling.dto.response.CreatedConsumerDto;
import com.arena.signaling.dto.response.CreatedProducerDto;
import com.arena.signaling.model.Participant;
import com.arena.signaling.repository.RedisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomManageService {


    private final Map<Long,Long> roomType = new ConcurrentHashMap<>();
    private final Map<Long, List<Participant>> rooms = new ConcurrentHashMap<>();
    private final Map<String, Long> userEmailToRoom = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RedisRepository redisRepository;

    public boolean isUserInRoom(String userEmail) {

        return userEmailToRoom.containsKey(userEmail);
    }

    public Long getRoomType(Long roomId){
        if(roomType.get(roomId) == null){
            return null;
        }
        return roomType.get(roomId);
    }

    public void addRoom(CreateRouterRequest req) {

        if (!rooms.containsKey(req.getRoomId())) {
            Map<String, Object> entries = redisRepository.getRoom(req.getRoomId());
            if(entries.isEmpty()) {
                return;
            }

            if(entries.get("type").toString().equals("1")){
                roomType.put(req.getRoomId(),2L);
                rooms.put(req.getRoomId(), new CopyOnWriteArrayList<>());
            }else{
                roomType.put(req.getRoomId(),4L);
                rooms.put(req.getRoomId(), new CopyOnWriteArrayList<>());
            }

        }
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
        return (participants == null) ? Collections.emptyList() : participants;
    }

    public void removeParticipant(String userEmail) throws JsonProcessingException {
        // Redis 에 삭제 요청
        if(userEmail == null){
            log.debug("[유저 삭제] 잘못된 요청입니다.");
            return;
        }
        Long roomId = userEmailToRoom.get(userEmail);
        if(roomId == null){
            log.debug("[유저 삭제] 방 및 유저를 찾을 수 없습니다.");
            return;
        }
        userEmailToRoom.remove(userEmail);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("roomId", roomId);
        map2.put("userEmail", userEmail);
        redisTemplate.convertAndSend("mediasoup:transport:disconnect", map2);


        List<Participant> participants = rooms.get(roomId);
        if(participants == null){
            log.debug("[유저 삭제] 해당 방에 참가자가 존재하지 않습니다.");
            return;
        }


        Participant removedParticipant = participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(userEmail))
                .findFirst()
                .orElse(null);

        if(removedParticipant == null){
            log.debug("[유저 삭제] 해당 방에 해당 유저가 존재하지 않습니다.");
            return;
        }
        participants.removeIf(p -> p.getProducerUserEmail().equals(userEmail));

        // 방에있는 다른 참가자들의 해당 유저와의 연결 상태 업데이트
        participants.forEach(participant -> {
            Map<String, Boolean> map = participant.getConsumerConnectedStatus();
            if (map != null && map.containsKey(userEmail) && map.get(userEmail) == Boolean.TRUE) {
                map.remove(userEmail);
            }
        });
        // 방이 비어있으면 제거
        if (participants.isEmpty()) {
            log.info("[유저 삭제] 방이 비어있으므로 해당 방을 삭제합니다 {} ", roomId);
            rooms.remove(roomId);
            roomType.remove(roomId);
        }

        log.info("[유저 삭제] {} 유저가 삭제됨 방 : {} ", userEmail, roomId);
        // 같은 방의 다른 참가자들에게 퇴장 알림
        // TODO : 같은방 참가자들에게 퇴장 알림 또는 pending 처리
//        getParticipants(roomId).forEach(participantSessionId -> {
//            messagingTemplate.convertAndSendToUser(
//                    participantSessionId,
//                    "/queue/participant-left",
//                    Map.of("userEmail", userEmail, "roomId", roomId)
//            );
//        });



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

            simpMessagingTemplate.convertAndSendToUser(
                    clientConnectionEstablishedDto.getConsumerUserEmail(),
                    "/queue/producer-connected",
                    "producer-connected"
            );
            return -2;
        }else {
            Map<String,Boolean> consumerStatus = participant.getConsumerConnectedStatus() ;
            if (consumerStatus==null) {
                consumerStatus = new HashMap<>();
            }
            consumerStatus.put(clientConnectionEstablishedDto.getProducerUserEmail(), true);
            participant.setConsumerConnectedStatus(consumerStatus);

            simpMessagingTemplate.convertAndSendToUser(
                    clientConnectionEstablishedDto.getConsumerUserEmail(),
                    "/queue/producer-connected",
                    clientConnectionEstablishedDto.getProducerUserEmail()
            );
        }


        return participants.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsProducerConnected()))
                .map(Participant::getConsumerConnectedStatus)
                .filter(Objects::nonNull)
                .flatMap(map -> map.values().stream())
                .filter(Boolean::booleanValue)
                .count();
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

    public void micOff(MediaControlDto mediaControlDto) {
        log.info("[micOff] {} ", mediaControlDto);
        List<Participant> participants =rooms.get(mediaControlDto.getRoomId());

        participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(mediaControlDto.getSpeaker()))
                .findFirst()
                .ifPresent(participant ->
                        redisTemplate.convertAndSend("mediasoup:producer:mic:off", participant.getProducerId()));
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
//
//
//    public void updateParticipantConsumerInfo(CreatedConsumerDto createdConsumerDto) {
//        List<Participant> participants = rooms.get(createdConsumerDto.getRoomId());
//        log.info("[updateParticipantConsumerInfo] {} ", participants);
//        Participant participant = participants.stream()
//                .filter(p -> p.getProducerUserEmail().equals(createdConsumerDto.getUserEmail()))
//                .findFirst()
//                .orElse(null);
//        if(participant == null){
//            log.info("[updateParticipantConsumerInfo] participant not found");
//            return;
//        }
//
//        if(participant.getConsumerConnectedStatus()==null){
//            participant.setConsumerConnectedStatus(new HashMap<>());
//        }
//
//        participant.getConsumerConnectedStatus().put(createdConsumerDto.getProducerUserEmail(), true);
//        log.info("[updateParticipantConsumerInfo] {} ", participant.getConsumerConnectedStatus());
//
//    }
}