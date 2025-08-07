package com.arena.signaling.service;

import com.arena.signaling.dto.*;
import com.arena.signaling.dto.response.*;
import com.arena.signaling.model.Participant;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MediasoupSubscribeService {
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomManageService roomManageService;
    private final SimpUserRegistry simpUserRegistry;

    public void createdRouter(CreatedRouterAndGetParticipantDto createdRouterAndGetParticipantDto) {
        createdRouterAndGetParticipantDto.setParticipants(roomManageService.getParticipants(createdRouterAndGetParticipantDto.getRoomId()));
        messagingTemplate.convertAndSendToUser(
            createdRouterAndGetParticipantDto.getUserEmail(),
            "/queue/router",
                createdRouterAndGetParticipantDto
        );
    }

    public void createdTransport(CreatedTransportDto createdTransportDto) {
        if(createdTransportDto.isProducer()){
            createdTransportDto.setType("producerTransportCreated");
            roomManageService.addParticipant(createdTransportDto);
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

    public void createdProducer(CreatedProducerDto createdProducerDto) {

        if(createdProducerDto.getError() != null){
            messagingTemplate.convertAndSendToUser(
                    createdProducerDto.getUserEmail(),
                    "/queue/producer",
                    createdProducerDto
            );

            return;
        }

        Long roomId = createdProducerDto.getRoomId();
        String userEmail = createdProducerDto.getUserEmail();


        // 1. Update participant info
        roomManageService.updateParticipantProducerInfo(createdProducerDto);

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

        List<Participant> participants = roomManageService.getParticipants(roomId);
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
                        } catch (Exception e) {
                            log.error("Failed to send newProducer notification to session {}: {}",
                                    p.getProducerUserEmail(), e.getMessage(), e);
                        }
                    });
        }

    }

    public void createdConsumer(CreatedConsumerDto createdConsumerDto) {

        if(createdConsumerDto.getError() == null){
            roomManageService.updateParticipantConsumerInfo(createdConsumerDto);
        }

        messagingTemplate.convertAndSendToUser(
            createdConsumerDto.getUserEmail (),
            "/queue/consumer",
                createdConsumerDto
        );
    }

    public void establishedTransport(ClientConnectionEstablishedDto clientConnectionEstablishedDto) {
        long result = roomManageService.updateParticipantConnectionInfo(clientConnectionEstablishedDto);

        if ((int) result == -1) {
            log.debug("connectedTransport - participant not found Error");
        } else {
            log.debug("connectedTransport - consumer connected {}", result);
            // 레디스에서 룸 사이즈 가져온다.
            if(result == 2) { // TODO : REDIS 에서 RoomSize 가져오기
                // TODO : redis 에 webrtcState 정보를 수정한다
                messagingTemplate.convertAndSend("/queue/"+ clientConnectionEstablishedDto.getRoomId(),"gameStart");
            }
        }

    }

    public void disconnectedTransport(TransportDisconnectedDto transportDisconnectedDto) {
        String userEmail = transportDisconnectedDto.getUserEmail();
        Long roomId = transportDisconnectedDto.getRoomId();
        
        log.info("[ Transport disconnected ] user: {} in room: {}", userEmail, roomId);
        
        try {
            SimpUser user = simpUserRegistry.getUser(userEmail);
            if (user != null) {
                for (SimpSession session : user.getSessions()) {
                    try {
                        WebSocketSession webSocketSession = (WebSocketSession) session.getUser();
                        if (webSocketSession != null && webSocketSession.isOpen()) {
                            webSocketSession.close(CloseStatus.NORMAL);
                            log.info("[disconnecting]: {}", userEmail);
                        }
                    } catch (Exception e) {
                        log.error("Error closing WebSocket session for user {}: {}", userEmail, e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error handling transport disconnection for user {}: {}", userEmail, e.getMessage(), e);
        }
    }

}