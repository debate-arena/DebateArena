package com.arena.common;

import com.arena.signaling.interceptor.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;



@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtChannelInterceptor jwtChannelInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // Simple message broker 활성화 - 클라이언트에게 메시지 전송용
    config.enableSimpleBroker("/sub", "/queue");
    // 애플리케이션에서 처리할 메시지의 prefix
    config.setApplicationDestinationPrefixes("/signaling");
    // 특정 사용자에게 메시지 전송용 prefix
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // STOMP endpoint 등록 - JWT authentication은 ChannelInterceptor에서 처리
    registry.addEndpoint("/signaling")
            .setAllowedOrigins("*")
            .withSockJS();

    // SockJS 없이도 연결 가능하도록
    registry.addEndpoint("/signaling")
            .setAllowedOrigins("*");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    // JWT 인증을 위한 ChannelInterceptor 등록
    registration.interceptors(jwtChannelInterceptor);
  }
}