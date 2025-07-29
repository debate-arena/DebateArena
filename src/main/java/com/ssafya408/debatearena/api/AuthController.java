package com.ssafya408.debatearena.api;

import com.ssafya408.debatearena.common.dto.ApiResponse;
import com.ssafya408.debatearena.secuirty.db.User;
import com.ssafya408.debatearena.secuirty.db.UserRepository;
import com.ssafya408.debatearena.secuirty.jwt.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "인증 API", description = "JWT 토큰 검증 및 로그아웃 관련 API")
public class AuthController {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    /**
     * 쿠키의 JWT 토큰을 확인하여 로그인 상태를 검증하는 API
     */
    @GetMapping("/verify")
    @Operation(
        summary = "JWT 토큰 검증",
        description = "쿠키에 저장된 JWT 토큰의 유효성을 검증하고 사용자 정보를 반환합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토큰 검증 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "authenticated": true,
                            "email": "user@example.com",
                            "nickname": "사용자닉네임",
                            "message": "로그인 상태입니다."
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토큰 없음 또는 유효하지 않음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "실패 예시",
                    value = """
                        {
                          "status": "error",
                          "data": "토큰이 없습니다."
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyToken(
            @Parameter(description = "HTTP 요청 객체", hidden = true) HttpServletRequest request) {
        Map<String, Object> responseData = new HashMap<>();
        
        try {
            // 쿠키에서 토큰 추출
            String token = extractTokenFromCookie(request);
            
            if (token == null) {
                responseData.put("authenticated", false);
                responseData.put("message", "토큰이 없습니다.");
                return ResponseEntity.ok(ApiResponse.error("토큰이 없습니다."));
            }

            // 토큰 유효성 검사
            if (!jwtProvider.validateToken(token)) {
                responseData.put("authenticated", false);
                responseData.put("message", "유효하지 않은 토큰입니다.");
                return ResponseEntity.ok(ApiResponse.error("유효하지 않은 토큰입니다."));
            }

            // 토큰에서 사용자 정보 추출
            String email = jwtProvider.getEmail(token);
            String role = jwtProvider.getRole(token);

            // SecurityContext에서 현재 인증 정보 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // 사용자 정보에서 닉네임 조회
            User user = userRepository.findByEmail(email).orElse(null);
            String nickname = user != null ? user.getNickname() : null;
            
            responseData.put("authenticated", true);
            responseData.put("email", email);
            responseData.put("nickname", nickname);
//            responseData.put("role", role);
            responseData.put("message", "로그인 상태입니다.");
            
            log.info("토큰 검증 성공: {} (닉네임: {})", email, nickname);
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
            
        } catch (Exception e) {
            log.error("토큰 검증 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("토큰 검증 중 오류가 발생했습니다."));
        }
    }

    /**
     * 로그아웃 요청 시 쿠키의 토큰을 만료시키는 API
     */
    @PostMapping("/logout")
    @Operation(
        summary = "로그아웃",
        description = "쿠키에 저장된 JWT 토큰을 만료시키고 로그아웃을 처리합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": "로그아웃이 완료되었습니다."
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그아웃 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "실패 예시",
                    value = """
                        {
                          "status": "error",
                          "data": "로그아웃 중 오류가 발생했습니다."
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<String>> logout(
            @Parameter(description = "HTTP 요청 객체", hidden = true) HttpServletRequest request,
            @Parameter(description = "HTTP 응답 객체", hidden = true) HttpServletResponse response) {
        try {
            // 쿠키에서 토큰 추출
            String token = extractTokenFromCookie(request);
            
            if (token != null) {
                String email = jwtProvider.getEmail(token);
                log.info("로그아웃 요청: {}", email);
            }

            // access_token 쿠키를 만료시킴
            Cookie cookie = new Cookie("access_token", null);
            cookie.setMaxAge(0); // 즉시 만료
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // 개발환경에서는 false, 프로덕션에서는 true
            response.addCookie(cookie);

            // SecurityContext 클리어
            SecurityContextHolder.clearContext();

            log.info("로그아웃 완료");
            
            return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("로그아웃 중 오류가 발생했습니다."));
        }
    }

    /**
     * 현재 로그인된 사용자 정보를 반환하는 API
     */
    @GetMapping("/me")
    @Operation(
        summary = "현재 사용자 정보 조회",
        description = "현재 로그인된 사용자의 정보를 반환합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "사용자 정보 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "authenticated": true,
                            "email": "user@example.com",
                            "nickname": "사용자닉네임",
                            "authorities": ["ROLE_USER"],
                            "message": "현재 로그인된 사용자 정보입니다."
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그인되지 않은 사용자",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "실패 예시",
                    value = """
                        {
                          "status": "error",
                          "data": "로그인되지 않은 사용자입니다."
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        Map<String, Object> responseData = new HashMap<>();
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                responseData.put("authenticated", false);
                responseData.put("message", "로그인되지 않은 사용자입니다.");
                return ResponseEntity.ok(ApiResponse.error("로그인되지 않은 사용자입니다."));
            }

            String email = authentication.getName();
            
            // 사용자 정보에서 닉네임 조회
            User user = userRepository.findByEmail(email).orElse(null);
            String nickname = user != null ? user.getNickname() : null;
            
            responseData.put("authenticated", true);
            responseData.put("email", email);
            responseData.put("nickname", nickname);
            responseData.put("authorities", authentication.getAuthorities());
            responseData.put("message", "현재 로그인된 사용자 정보입니다.");
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("사용자 정보 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 쿠키에서 토큰을 추출하는 헬퍼 메서드
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("access_token")) {
                return cookie.getValue();
            }
        }
        return null;
    }
} 