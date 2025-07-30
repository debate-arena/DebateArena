package com.arena.test04spring.controller;

import com.arena.test04spring.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> createToken(@RequestBody TokenRequest request) {
        try {
            String token = jwtUtil.createToken(
                request.getUserEmail(), 
                24 * 60 * 60 * 1000L // 24시간
            );
            
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            log.error("Token creation failed", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Token creation failed"));
        }
    }

    public static class TokenRequest {
        private String userEmail;



        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }
    }
}
/*
* curl -X POST http://localhost:8080/api/auth/token -H "Content-Type: application/json" -d "{\"userEmail\":\"test@example.com\"}"
* curl -X POST http://localhost:8080/api/auth/token -H "Content-Type: application/json" -d "{\"userEmail\":\"test@example.com\"}"
* */