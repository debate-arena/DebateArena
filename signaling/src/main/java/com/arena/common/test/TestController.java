package com.arena.common.test;

import com.arena.common.dto.ApiResponse;
import com.arena.common.security.jwt.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
        summary = "테스트용 JWT 토큰 발급",
        description = "Google OAuth 인증 없이 이메일만으로 JWT 토큰을 발급받습니다. 개발 및 테스트 목적으로만 사용하세요."
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
                            "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNjM5NTg5NjAwLCJleHAiOjE2Mzk2NzYwMDB9.xxx",
                            "email": "test@example.com",
                            "issuedAt": "2024-01-01T12:00:00",
                            "message": "테스트용 JWT 토큰이 발급되었습니다."
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
        @RequestBody @Valid TestLoginRequest request
    ) {
        log.info("[테스트 토큰 발급] 요청 - 이메일: {}", request.getEmail());
        
        try {
            // JWT 토큰 생성
            String token = jwtProvider.createToken(request.getEmail());
            
            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("email", request.getEmail());
            responseData.put("issuedAt", LocalDateTime.now().toString());
            responseData.put("message", "테스트용 JWT 토큰이 발급되었습니다.");
            
            log.info("[테스트 토큰 발급] 성공 - 이메일: {} | 토큰 길이: {}", 
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