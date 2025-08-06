package com.ssafya408.matching.common.test;

import com.ssafya408.matching.api.dto.ApiResponse;
import com.ssafya408.matching.common.secuirty.jwt.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트용 API 컨트롤러
 * Google OAuth 인증 없이 JWT 토큰을 발급받을 수 있는 테스트 엔드포인트 제공
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
@Tag(name = "테스트 API", description = "개발 및 테스트용 API (프로덕션 환경에서는 비활성화 필요)")
@Slf4j
public class TestController {

    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    @Operation(
        summary = "테스트용 JWT 토큰 발급 (쿠키 저장)",
        description = "Google OAuth 인증 없이 이메일만으로 JWT 토큰을 발급받고 access_token 쿠키에 저장합니다. 개발 및 테스트 목적으로만 사용하세요."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "JWT 토큰 발급 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "email": "test@example.com",
                            "issuedAt": "2024-01-01T12:00:00",
                            "message": "테스트용 JWT 토큰이 쿠키에 저장되었습니다.",
                            "cookieName": "access_token"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (이메일 형식 오류)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "실패 예시",
                    value = """
                        {
                          "status": "error",
                          "data": "올바른 이메일 형식을 입력해주세요."
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateTestToken(
        @RequestBody @Valid TestLoginRequest request,
        HttpServletResponse response
    ) {
        log.info("[테스트 토큰 발급] 요청 - 이메일: {}", request.getEmail());
        
        try {
            // JWT 토큰 생성
            String token = jwtProvider.createToken(request.getEmail());
            
            // 쿠키에 토큰 설정
            Cookie tokenCookie = new Cookie("access_token", token);
            tokenCookie.setHttpOnly(true); // XSS 공격 방지
            tokenCookie.setPath("/"); // 모든 경로에서 사용 가능
            tokenCookie.setMaxAge(24 * 60 * 60); // 24시간 (초 단위)
            tokenCookie.setSecure(false); // 개발환경에서는 false (HTTPS가 아니므로)
            // tokenCookie.setAttribute("SameSite", "Lax"); // CSRF 방지
            
            response.addCookie(tokenCookie);
            
            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", request.getEmail());
            responseData.put("issuedAt", LocalDateTime.now().toString());
            responseData.put("message", "테스트용 JWT 토큰이 쿠키에 저장되었습니다.");
            responseData.put("cookieName", "access_token");
            
            log.info("[테스트 토큰 발급] 성공 - 이메일: {} | 토큰 길이: {} | 쿠키 설정 완료", 
                request.getEmail(), token.length());
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
            
        } catch (Exception e) {
            log.error("[테스트 토큰 발급] 실패 - 이메일: {} | 오류: {}", 
                request.getEmail(), e.getMessage(), e);
            
            return ResponseEntity.ok(
                ApiResponse.error("JWT 토큰 발급 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }

    @PostMapping("/logout")
    @Operation(
        summary = "테스트용 로그아웃",
        description = "access_token 쿠키를 삭제하여 로그아웃을 수행합니다."
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
                          "data": {
                            "message": "로그아웃이 완료되었습니다.",
                            "loggedOutAt": "2024-01-01T12:00:00"
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(HttpServletResponse response) {
        log.info("[테스트 로그아웃] 요청");
        
        try {
            // 쿠키 삭제 (MaxAge를 0으로 설정)
            Cookie tokenCookie = new Cookie("access_token", "");
            tokenCookie.setHttpOnly(true);
            tokenCookie.setPath("/");
            tokenCookie.setMaxAge(0); // 즉시 만료
            tokenCookie.setSecure(false);
            
            response.addCookie(tokenCookie);
            
            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "로그아웃이 완료되었습니다.");
            responseData.put("loggedOutAt", LocalDateTime.now().toString());
            
            log.info("[테스트 로그아웃] 성공 - 쿠키 삭제 완료");
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
            
        } catch (Exception e) {
            log.error("[테스트 로그아웃] 실패 - 오류: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(
                ApiResponse.error("로그아웃 중 오류가 발생했습니다: " + e.getMessage())
            );
        }
    }

    /**
     * 테스트용 로그인 요청 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "테스트용 로그인 요청")
    public static class TestLoginRequest {
        
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식을 입력해주세요.")
        @Schema(
            description = "사용자 이메일", 
            example = "test@example.com",
            required = true
        )
        private String email;
    }
}