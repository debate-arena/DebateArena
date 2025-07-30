package com.arena.test04spring.service;

import com.arena.test04spring.dto.*;
import com.arena.test04spring.model.Participant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediasoupSubscriber {
    private final Map<Long, List<Participant>> rooms;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void createdRouter(RouterInfo routerInfo) throws IOException {
        routerInfo.setParticipants(rooms.get(routerInfo.getRoomId()));
        messagingTemplate.convertAndSendToUser(
            routerInfo.getUserEmail(),
            "/queue/router", 
            routerInfo
        );
    }

    public void createdTransport(TransportInfo transportInfo) throws IOException {
        if(transportInfo.isProducer()){
            transportInfo.setType("producerTransportCreated");
        }else{
            transportInfo.setType("consumerTransportCreated");
        }

        messagingTemplate.convertAndSendToUser(
            transportInfo.getUserEmail(),
            "/queue/transport",
            transportInfo
        );
    }

    public void connectedTransport(TransportConnectedResponseDto transportConnectedResponseDto) throws IOException {
        messagingTemplate.convertAndSendToUser(
            transportConnectedResponseDto.getUserEmail(),
            "/queue/transport-connected",
            transportConnectedResponseDto
        );
    }

    public void createdProducer(ProducerCreated producerCreated) throws IOException {
        Long roomId = producerCreated.getRoomId();
        String userEmail = producerCreated.getUserEmail();
        
        boolean participantAdded = false;
        
        try {
            // 1. 먼저 participant를 rooms에 추가
            List<Participant> participants = rooms.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
            
            // 중복 확인 후 추가
            boolean exists = participants.stream()
                    .anyMatch(p -> p.getProducerUserEmail().equals(userEmail));
            
            if (!exists) {
                Participant participant = Participant.builder()
                        .producerUserEmail(userEmail)
                        .producerId(producerCreated.getProducerId())
                        .build();
                
                participants.add(participant);
                participantAdded = true;
            }
            
            // 2. 본인에게 producer 생성 완료 응답
            messagingTemplate.convertAndSendToUser(
                    userEmail,
                "/queue/producer",
                producerCreated
            );
            
            // 3. 다른 참가자들에게 새로운 producer 알림
            notifyNewProducerToRoom(roomId, userEmail,
                                   producerCreated.getProducerId(), producerCreated.getKind());
            
        } catch (Exception e) {
            // 실패 시 rollback
            if (participantAdded) {
                List<Participant> participants = rooms.get(roomId);
                if (participants != null) {
                    participants.removeIf(p -> p.getProducerUserEmail().equals(userEmail));
                }
            }
            throw e;
        }
    }

    public void createdConsumer(ConsumerInfo consumerInfo) throws IOException {
        messagingTemplate.convertAndSendToUser(
            consumerInfo.getUserEmail (),
            "/queue/consumer",
            consumerInfo
        );
    }

    private void notifyNewProducerToRoom(Long roomId, String producerUserEmail, String producerId, String kind) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode notification = mapper.createObjectNode();
        notification.put("producerId", producerId);
        notification.put("producerUserEmail", producerUserEmail);
        notification.put("kind", kind);

        List<Participant> participants = rooms.get(roomId);
        if (participants != null) {
            participants.stream()
                    .filter(p -> !p.getProducerUserEmail().equals(producerUserEmail))
                    .forEach(p -> {
                        try {
                            messagingTemplate.convertAndSendToUser(
                                p.getProducerUserEmail(),
                                "/queue/new-producer",
                                notification
                            );
                        } catch (Exception e) {
                            log.error("Failed to send newProducer notification to session {}: {}", 
                                     p.getProducerUserEmail(), e.getMessage(), e);
                        }
                    });
        }
    }
}