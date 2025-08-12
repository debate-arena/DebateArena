package com.arena.signaling.test;

import com.arena.common.security.jwt.JwtProvider;
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

    private final JwtProvider jwtUtil;

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> createToken(@RequestBody TokenRequest request) {
        try {
            String token = jwtUtil.createToken(request.getUserEmail());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            log.error("Token creation failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Token creation failed"));
        }
    }
    @PostMapping("/valid")
    public ResponseEntity<Map<String, Boolean>> IsTokenValid(@RequestParam String request) {
        try {
            Boolean token = jwtUtil.validateToken(request);
            return ResponseEntity.ok(Map.of("isValid", token));
        } catch (Exception e) {
            log.error("Token creation failed", e);
            return ResponseEntity.ok(Map.of("isValid", false));
        }
    }

//    @PostMapping("/matching")
//    public ResponseEntity<Map<String, Boolean>> req(@RequestParam String request) {
//        try {
//            String url = "https://jsonplaceholder.typicode.com/posts/1";
//
//            // GET 요청
//            return ResponseEntity.ok(Map.of("isValid", token));
//        } catch (Exception e) {
//            log.error("Token creation failed", e);
//            return ResponseEntity.ok(Map.of("isValid", false));
//        }
//    }

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