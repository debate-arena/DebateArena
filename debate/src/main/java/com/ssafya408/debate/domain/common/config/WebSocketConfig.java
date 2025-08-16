package com.ssafya408.debate.domain.common.config;

import com.ssafya408.debate.domain.api.interceptor.JwtChannelInterceptor;
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
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*");
//        .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/sub","/queue");
    registry.setApplicationDestinationPrefixes("/pub");
    registry.setUserDestinationPrefix("/user");
  }
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    // JWT 인증을 위한 ChannelInterceptor 등록
//    registration.interceptors(jwtChannelInterceptor);
  }

}