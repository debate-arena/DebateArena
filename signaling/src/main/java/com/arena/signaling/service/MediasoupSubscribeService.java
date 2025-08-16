package com.arena.signaling.service;

import com.arena.signaling.dto.*;
import com.arena.signaling.dto.response.*;
import com.arena.signaling.model.Participant;
import com.arena.signaling.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediasoupSubscribeService {
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomManageService roomManageService;
    private final SimpUserRegistry simpUserRegistry;
    private final RedisRepository redisRepository;

    public void createdRouter(CreatedRouterAndGetParticipantDto createdRouterAndGetParticipantDto) {
        createdRouterAndGetParticipantDto.setParticipants(roomManageService.getParticipants(createdRouterAndGetParticipantDto.getRoomId()));
        messagingTemplate.convertAndSendToUser(
            createdRouterAndGetParticipantDto.getUserEmail(),
            "/queue/router",
                createdRouterAndGetParticipantDto
        );
    }

    public void createdTransport(CreatedTransportDto createdTransportDto) {
        roomManageService.addParticipant(createdTransportDto);
        if(createdTransportDto.isProducer()){
            createdTransportDto.setType("producerTransportCreated");
        }else{
            createdTransportDto.setType("consumerTransportCreated");
        }

        messagingTemplate.convertAndSendToUser(
            createdTransportDto.getUserEmail(),
            "/queue/transport",
                createdTransportDto
        );
    }

    public void connectedTransport(TransportConnectedResponseDto transportConnectedResponseDto) {
        messagingTemplate.convertAndSendToUser(
            transportConnectedResponseDto.getUserEmail(),
            "/queue/transport-connected",
            transportConnectedResponseDto
        );
    }

    public synchronized void createdProducer(CreatedProducerDto createdProducerDto) {
        log.info("[createdProducer] {}", createdProducerDto);
        roomManageService.updateProducerId(createdProducerDto);
        log.info("[createdProducer] 업데이트 완료");

        Long roomId = createdProducerDto.getRoomId();
        String userEmail = createdProducerDto.getUserEmail();

        // 2. 본인에게 알림
        messagingTemplate.convertAndSendToUser(
                userEmail,
            "/queue/producer",
                createdProducerDto
        );

        // 3. 해당 방의 다른 참가자들에게 새로운 producer 알림
        NewProducerResponseDto newProducerResponseDto = NewProducerResponseDto.builder()
                .producerId(createdProducerDto.getProducerId())
                .producerUserEmail(createdProducerDto.getUserEmail())
                .kind(createdProducerDto.getKind())
                .build();
        // TODO : BroadCast 방식 변경
//        messagingTemplate.convertAndSend("/queue/new-producer/"+roomId
//                ,newProducerResponseDto);

        List<Participant> participants = roomManageService.getAllParticipants(roomId);
        if (participants != null) {
            participants.stream()
                    .filter(p -> !p.getProducerUserEmail().equals(userEmail))
                    .forEach(p -> {
                        try {
                            messagingTemplate.convertAndSendToUser(
                                    p.getProducerUserEmail(),
                                    "/queue/new-producer",
                                    newProducerResponseDto
                            );
                            log.info("[createdProducer] 메시지 전송 완료 TO {}", p.getProducerUserEmail());
                        } catch (Exception e) {
                            log.error("Failed to send newProducer notification to session {}: {}",
                                    p.getProducerUserEmail(), e.getMessage(), e);
                        }
                    });
        }

    }

    public void createdConsumer(CreatedConsumerDto createdConsumerDto) {
        messagingTemplate.convertAndSendToUser(
            createdConsumerDto.getUserEmail (),
            "/queue/consumer",
                createdConsumerDto
        );
    }

    public void establishedTransport(ClientConnectionEstablishedDto clientConnectionEstablishedDto) {
        Long val = roomManageService.updateParticipantConnectionInfo(clientConnectionEstablishedDto);
        if(val==null){
            log.debug("[established transport is null]");
            return;
        }
        long result = val;

        switch ((int) result) {
            case -1:
                log.debug("[Transport 연결됨] ERROR 유저가 방에 존재하지 않음.");
                break;
            case -2:
                log.debug("[Transport 연결됨] 유저 연결됨 (PRODUCER) {}", result);
                break;
            default:
                long roomType =roomManageService.getRoomType(clientConnectionEstablishedDto.getRoomId());
                if(result == roomType){
                    redisRepository.updateField(clientConnectionEstablishedDto.getRoomId());
                    messagingTemplate.convertAndSend(
                            "/sub/room/" + clientConnectionEstablishedDto.getRoomId() + "/connected",
                            "gameStart"
                    );
                    log.debug("[참가자의 연결이 완료됨]");
                }
        }
    }

    public void disconnectedTransport(TransportDisconnectedDto transportDisconnectedDto) {
        String userEmail = transportDisconnectedDto.getUserEmail();
        Long roomId = transportDisconnectedDto.getRoomId();
        
        log.info("[ Transport 연결 해제됨 유저 삭제 요청] user: {} in room: {}", userEmail, roomId);
        
        try {
            SimpUser user = simpUserRegistry.getUser(userEmail);
            if (user != null) {
                for (SimpSession session : user.getSessions()) {
                    try {
                        WebSocketSession webSocketSession = (WebSocketSession) session.getUser();
                        if (webSocketSession != null && webSocketSession.isOpen()) {
                            webSocketSession.close(CloseStatus.NORMAL);
                            log.info("[유저 삭제 요청]: {}", userEmail);
                        }
                    } catch (Exception e) {
                        log.error("[유저 삭제 요청] {}: {}", userEmail, e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[유저 삭제 요청] {}: {}", userEmail, e.getMessage(), e);
        }
    }

}