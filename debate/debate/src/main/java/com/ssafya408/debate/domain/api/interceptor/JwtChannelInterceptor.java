package com.ssafya408.debate.domain.api.interceptor;

import com.ssafya408.debate.domain.common.security.jwt.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    public JwtChannelInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // STOMP CONNECT 프레임에서 Authorization 헤더 추출
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("JWT token not found in STOMP CONNECT headers");
                throw new IllegalArgumentException("JWT token not found in STOMP CONNECT headers");
            }
            
            String token = authHeader.substring(7);
            
            if (!jwtProvider.validateToken(token)) {
                log.warn("Invalid JWT token in STOMP CONNECT headers");
                throw new IllegalArgumentException("Invalid JWT token");
            }
            
            // 토큰에서 사용자 정보 추출
            String userEmail = jwtProvider.getEmail(token);
            
            // Spring Security Authentication 객체 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userEmail, 
                null, 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            accessor.setUser(authentication);
            accessor.getSessionAttributes().put("userEmail", userEmail);
            accessor.getSessionAttributes().put("token", token);
            
            log.info("JWT authentication successful for user: {}", userEmail);
        }
        
        return message;
    }
}