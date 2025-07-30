//package com.arena.test04spring.config;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.server.ServerHttpRequest;
//import org.springframework.http.server.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.WebSocketHandler;
//import org.springframework.web.socket.server.HandshakeInterceptor;
//
//import java.net.URI;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class JwtHandshakeInterceptor implements HandshakeInterceptor {
//
//    private final JwtUtil jwtUtil;
//
//    @Override
//    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
//                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
//
//        String token = extractTokenFromRequest(request);
//
//        if (token == null) {
//            log.warn("JWT token not found in WebSocket handshake");
//            return false;
//        }
//
//        if (!jwtUtil.validateToken(token)) {
//            log.warn("Invalid JWT token in WebSocket handshake");
//            return false;
//        }
//
//        // 토큰에서 사용자 정보 추출하여 세션 속성에 저장
//        String userEmail = jwtUtil.getEmail(token);
//
//        attributes.put("userEmail", userEmail);
//        attributes.put("token", token);
//        return true;
//    }
//
//    @Override
//    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
//                               WebSocketHandler wsHandler, Exception exception) {
//        // 핸드셰이크 완료 후 처리할 로직이 있다면 구현
//    }
//
//    private String extractTokenFromRequest(ServerHttpRequest request) {
//        URI uri = request.getURI();
//        String query = uri.getQuery();
//
//        if (query != null) {
//            String[] params = query.split("&");
//            for (String param : params) {
//                String[] keyValue = param.split("=");
//                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
//                    return keyValue[1];
//                }
//            }
//        }
//
//        // Authorization 헤더에서 토큰 추출
//        String authHeader = request.getHeaders().getFirst("Authorization");
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            return authHeader.substring(7);
//        }
//
//        return null;
//    }
//}