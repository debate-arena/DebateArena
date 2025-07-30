package com.arena.test04spring.subscriber;


import com.arena.test04spring.dto.*;
import com.arena.test04spring.service.MediasoupPublisher;
import com.arena.test04spring.service.MediasoupSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SocketMessageListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final MediasoupPublisher mediasoupPublisher;
    private final MediasoupSubscriber mediasoupSubscriber;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 메시지 처리 로직
        String channel = new String(message.getChannel());
        byte[] bodyBytes = message.getBody();
        ObjectMapper mapper = new ObjectMapper();

        switch (channel) {
            case "mediasoup:router:created":
                try {
                    RouterInfo routerInfo = mapper.readValue(bodyBytes, RouterInfo.class);
                    mediasoupSubscriber.createdRouter(routerInfo);
                    log.debug("[router created]");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case "mediasoup:transport:created":
                try {
                    TransportInfo transportList = mapper.readValue(bodyBytes, TransportInfo.class);
                    mediasoupSubscriber.createdTransport(transportList);
                    log.debug("[transport created]");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case "mediasoup:transport:connected":
                try {
                    TransportConnectedResponseDto transportConnectedResponseDto = mapper
                            .readValue(bodyBytes, TransportConnectedResponseDto.class);
                    mediasoupSubscriber.connectedTransport(transportConnectedResponseDto);
                    log.debug("[transport connected]");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case "mediasoup:producer:created":
                try {
                    ProducerCreated producerCreated = mapper
                            .readValue(bodyBytes, ProducerCreated.class);
                    mediasoupSubscriber.createdProducer(producerCreated);
                    log.debug("[producer created]");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "mediasoup:consumer:created":
                try {
                    ConsumerInfo consumerInfo = mapper
                            .readValue(bodyBytes, ConsumerInfo.class);
                    mediasoupSubscriber.createdConsumer(consumerInfo);
                    log.debug("[consumer created]");
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


