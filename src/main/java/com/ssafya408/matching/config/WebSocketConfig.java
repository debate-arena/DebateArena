package com.ssafya408.matching.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*");
    // .withSockJS(); 테스트에서 해당 코드가 있으면 제대로 동작하지 않는다ㅜ
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/sub", "/queue"); // 서버 -> 클라이언트로 보낸다 (/queue 추가)
    registry.setApplicationDestinationPrefixes("/pub"); // 클라이언트 -> 서버로 전송할 때 붙이는 prefix
    registry.setUserDestinationPrefix("/user"); // 유저 개별 전송용 prefix 1:1 메시지에 사용
  }

}
