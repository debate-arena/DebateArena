package com.arena.signaling.listener;


import com.arena.signaling.dto.response.*;
import com.arena.signaling.service.RoomManageService;
import com.arena.signaling.dto.*;
import com.arena.signaling.service.MediasoupPublishService;
import com.arena.signaling.service.MediasoupSubscribeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final MediasoupPublishService mediasoupPublishService;
    private final MediasoupSubscribeService mediasoupSubscribeService;
    private final RoomManageService roomManageService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 메시지 처리 로직
        String channel = new String(message.getChannel());
        byte[] bodyBytes = message.getBody();
        ObjectMapper mapper = new ObjectMapper();

        switch (channel) {
            case "mediasoup:router:created":
                try {
                    CreatedRouterAndGetParticipantDto createdRouterAndGetParticipantDto = mapper.readValue(bodyBytes, CreatedRouterAndGetParticipantDto.class);
                    mediasoupSubscribeService.createdRouter(createdRouterAndGetParticipantDto);
                    log.debug("[라우터 정보 응답] {}",createdRouterAndGetParticipantDto);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case "mediasoup:transport:created":
                try {
                    CreatedTransportDto transportList = mapper.readValue(bodyBytes, CreatedTransportDto.class);
                    mediasoupSubscribeService.createdTransport(transportList);
                    log.debug("[transport created]");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case "mediasoup:transport:connected":
                try {
                    TransportConnectedResponseDto transportConnectedResponseDto = mapper
                            .readValue(bodyBytes, TransportConnectedResponseDto.class);
                    mediasoupSubscribeService.connectedTransport(transportConnectedResponseDto);
                    log.debug("[transport connected]");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case "mediasoup:producer:created":
                try {
                    CreatedProducerDto createdProducerDto = mapper
                            .readValue(bodyBytes, CreatedProducerDto.class);
                    mediasoupSubscribeService.createdProducer(createdProducerDto);
                    log.debug("[producer created]");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "mediasoup:consumer:created":
                try {
                    CreatedConsumerDto createdConsumerDto = mapper
                            .readValue(bodyBytes, CreatedConsumerDto.class);
                    mediasoupSubscribeService.createdConsumer(createdConsumerDto);
                    log.debug("[consumer created]");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "mediasoup:transport:established":
                try {
                    ClientConnectionEstablishedDto clientConnectionEstablishedDto = mapper
                            .readValue(bodyBytes, ClientConnectionEstablishedDto.class);
                    mediasoupSubscribeService.establishedTransport(clientConnectionEstablishedDto);
                    log.debug("[transport established]");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "mediasoup:transport:disconnected":
                try {
                    TransportDisconnectedDto transportDisconnectedDto = mapper
                            .readValue(bodyBytes, TransportDisconnectedDto.class);
                    mediasoupSubscribeService.disconnectedTransport(transportDisconnectedDto);
                    log.debug("[transport disconnected]");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "signaling:mic:on":
                try {
                    MediaControlDto mediaControlDto = mapper
                            .readValue(bodyBytes, MediaControlDto.class);
                    roomManageService.micOn(mediaControlDto);
                    log.debug("[transport disconnected]");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

//            case "marker-sync":
//                try {
//                    MarkerInfoMessage markerInfoMessage = objectMapper.readValue(message.getBody(), MarkerInfoMessage.class);
//                    markerService.sync(markerInfoMessage);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                break;
//
//            case "marker-update":
//                try {
//                    MarkerInfoMessage markerInfoMessage = objectMapper.readValue(message.getBody(), MarkerInfoMessage.class);
//                    markerService.update(markerInfoMessage);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                break;

            default:
                System.out.println("Unknown topic: " + channel);
        }
    }
}


