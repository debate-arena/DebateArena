package com.arena.signaling.service;

import com.arena.signaling.dto.*;
import com.arena.signaling.dto.request.CreateRouterRequest;
import com.arena.signaling.dto.response.CreatedConsumerDto;
import com.arena.signaling.dto.response.CreatedProducerDto;
import com.arena.signaling.dto.response.CreatedTransportDto;
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
import java.util.concurrent.locks.ReentrantLock;

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
    private final Map<Long, ReentrantLock> roomLocks = new ConcurrentHashMap<>();

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
            if(entries.get("type").toString().equals("0")){
                roomType.put(req.getRoomId(),2L);
                rooms.put(req.getRoomId(), new CopyOnWriteArrayList<>());
            }else{
                roomType.put(req.getRoomId(),4L);
                rooms.put(req.getRoomId(), new CopyOnWriteArrayList<>());
            }
        }
        userEmailToRoom.put(req.getUserEmail(), req.getRoomId());
    }

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
                    .build();
            participants.add(participant);
            log.debug("[Transport가 생성됨] [participant에 추가] isProducer {} email : {} room : {}"
                    ,createdTransportDto.isProducer(), createdTransportDto.getUserEmail(), createdTransportDto.getRoomId());
        } else {
            log.debug("[Transport가 생성됨] [이미 participant에 존재] isProducer {} email : {} room : {}"
                    ,createdTransportDto.isProducer(), createdTransportDto.getUserEmail(), createdTransportDto.getRoomId());
        }

        userEmailToRoom.put(createdTransportDto.getUserEmail(), createdTransportDto.getRoomId());
        log.debug("[참가자 추가 user: {} room: {}]", createdTransportDto.getUserEmail(), createdTransportDto.getRoomId());
    }

    public List<Participant> getParticipants(Long roomId) {
        ReentrantLock lock = roomLocks.computeIfAbsent(roomId, k -> new ReentrantLock());
        lock.lock();
        try {
            List<Participant> participants = rooms.get(roomId);
            List<Participant> filteredParticipants = participants.stream()
                    .filter(p -> !p.getProducerId().isEmpty())
                    .toList();
            if (filteredParticipants.isEmpty()) {
                return null;
            }
            return filteredParticipants;
        }catch (Exception e){
            log.error("Failed to get participants: {}", e.getMessage(), e);
            return null;
        }finally {
            lock.unlock();
        }

    }

    public List<Participant> getAllParticipants(Long roomId) {
        ReentrantLock lock = roomLocks.computeIfAbsent(roomId, k -> new ReentrantLock());
        lock.lock();
        try {
            List<Participant> participants = rooms.get(roomId);
            List<Participant> filteredParticipants = participants.stream()
                    .filter(p -> !p.getProducerUserEmail().isEmpty())
                    .toList();
            if (filteredParticipants.isEmpty()) {
                return null;
            }
            return filteredParticipants;
        }catch (Exception e){
            log.error("Failed to get participants: {}", e.getMessage(), e);
            return null;
        }finally {
            lock.unlock();
        }

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

        if (participants.isEmpty()) {
            log.info("[유저 삭제] 방이 비어있으므로 해당 방을 삭제합니다 {} ", roomId);
            rooms.remove(roomId);
            roomType.remove(roomId);
        }

        log.info("[유저 삭제] {} 유저가 삭제됨 방 : {} ", userEmail, roomId);

        // TODO : Producer 라면, 같은방 참가자들에게 퇴장 알림 또는 pending 처리

    }

    public Long updateParticipantConnectionInfo(ClientConnectionEstablishedDto clientConnectionEstablishedDto) {

        log.debug("[updateParticipantConnectionInfo] {}", clientConnectionEstablishedDto);

        Long roomId = clientConnectionEstablishedDto.getRoomId();
        if (roomId == null || !rooms.containsKey(roomId)) return -1L;
        List<Participant> participants = rooms.get(roomId);
        if (participants == null) return -1L;

        String consumerEmail = clientConnectionEstablishedDto.getConsumerUserEmail();

        Participant participant = participants.stream()
                .filter(p -> consumerEmail.equals(p.getProducerUserEmail()))
                .findFirst()
                .orElse(null);

        if (participant == null) return -1L;

        if (clientConnectionEstablishedDto.getIsProducer()) {
            participant.setProducerConnectedStatus(true);
            simpMessagingTemplate.convertAndSendToUser(
                    clientConnectionEstablishedDto.getConsumerUserEmail(),
                    "/queue/producer-connected",
                    "producer-connected"
            );
        } else {
            participant.setConsumerConnectedStatus(true);
            //            simpMessagingTemplate.convertAndSendToUser(
            //                    clientConnectionEstablishedDto.getConsumerUserEmail(),
            //                    "/queue/producer-connected",
            //                    clientConnectionEstablishedDto.getProducerUserEmail()
            //            );
        }

        Long count = Optional.ofNullable(participants) // participants가 null일 경우 빈 리스트로 대체
                .orElse(Collections.emptyList())
                .stream()
                .filter(p -> Boolean.TRUE.equals(p.getProducerConnectedStatus())) // null-safe
                .filter(p -> Boolean.TRUE.equals(p.getConsumerConnectedStatus())) // null-safe
                .count();

        return count;
    }



    public void micOn(MediaControlDto mediaControlDto) {
        log.info("[micOn] {} ", mediaControlDto);
        List<Participant> participants =rooms.get(mediaControlDto.getRoomId());

        participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(mediaControlDto.getSpeaker()))
                .findFirst()
                .ifPresent(participant ->{
                            mediaControlDto.setProducerId(participant.getProducerId());
                            redisTemplate.convertAndSend("mediasoup:producer:mic:on", mediaControlDto);
                        }
                        );
    }

    public void micOff(MediaControlDto mediaControlDto) {
        log.info("[micOff] {} ", mediaControlDto);
        List<Participant> participants =rooms.get(mediaControlDto.getRoomId());

        participants.stream()
                .filter(p -> p.getProducerUserEmail().equals(mediaControlDto.getSpeaker()))
                .findFirst()
                .ifPresent(participant ->{
                    mediaControlDto.setProducerId(participant.getProducerId());
                    redisTemplate.convertAndSend("mediasoup:producer:mic:off", mediaControlDto);
                        }
                );
    }

    public void updateProducerId(CreatedProducerDto createdProducerDto) {

        ReentrantLock lock = roomLocks.computeIfAbsent(createdProducerDto.getRoomId(), k -> new ReentrantLock());
        lock.lock();
        try {
            List<Participant> participants = rooms.get(createdProducerDto.getRoomId());
            Participant participant = participants.stream()
                    .filter(p -> createdProducerDto.getUserEmail().equals(p.getProducerUserEmail()))
                    .findFirst()
                    .orElse(null);
            if(participant == null) return;
            participant.setProducerId(createdProducerDto.getProducerId());
        }catch (Exception e){
            log.error("Failed to update producer id: {}", e.getMessage(), e);
        }finally {
            lock.unlock();
        }
    }

    public Boolean isRoomValid(Long roomId) {
        Map<String, Object> entries = redisRepository.getRoom(roomId);
        return !entries.isEmpty();
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