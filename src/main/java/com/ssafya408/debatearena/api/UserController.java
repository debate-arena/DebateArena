package com.ssafya408.debatearena.api;

import com.ssafya408.debatearena.api.dto.NicknameUpdateRequest;
import com.ssafya408.debatearena.common.dto.ApiResponse;
import com.ssafya408.debatearena.secuirty.db.User;
import com.ssafya408.debatearena.secuirty.db.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "사용자 API", description = "사용자 정보 관리 관련 API")
public class UserController {

    private final UserRepository userRepository;

    /**
     * 닉네임 중복 확인 API
     */
    @GetMapping("/nickname/check")
    @Operation(
        summary = "닉네임 중복 확인",
        description = "입력된 닉네임이 이미 사용 중인지 확인합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "중복 확인 완료",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "사용 가능한 닉네임",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "available": true,
                            "message": "사용 가능한 닉네임입니다."
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkNickname(
            @Parameter(description = "확인할 닉네임", required = true)
            @RequestParam @NotBlank @Size(min = 2, max = 16) String nickname) {
        
        Map<String, Object> responseData = new HashMap<>();
        
        try {
            // 닉네임 중복 확인
            boolean exists = userRepository.existsByNickname(nickname);
            
            responseData.put("available", !exists);
            responseData.put("nickname", nickname);
            
            if (exists) {
                responseData.put("message", "이미 사용 중인 닉네임입니다.");
                log.info("닉네임 중복 확인 - 사용 불가: {}", nickname);
                return ResponseEntity.ok(ApiResponse.error("이미 사용 중인 닉네임입니다."));
            } else {
                responseData.put("message", "사용 가능한 닉네임입니다.");
                log.info("닉네임 중복 확인 - 사용 가능: {}", nickname);
                return ResponseEntity.ok(ApiResponse.success(responseData));
            }
            
        } catch (Exception e) {
            log.error("닉네임 중복 확인 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("닉네임 중복 확인 중 오류가 발생했습니다."));
        }
    }

    /**
     * 닉네임 업데이트 API
     */
    @PutMapping("/nickname")
    @Operation(
        summary = "닉네임 업데이트",
        description = "현재 로그인된 사용자의 닉네임을 업데이트합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "닉네임 업데이트 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "message": "닉네임이 성공적으로 업데이트되었습니다.",
                            "nickname": "새로운닉네임"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "닉네임 업데이트 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "실패 예시",
                    value = """
                        {
                          "status": "error",
                          "data": "이미 사용 중인 닉네임입니다."
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNickname(
            @Parameter(description = "업데이트할 닉네임", required = true)
            @RequestBody @Valid NicknameUpdateRequest request) {
        
        Map<String, Object> responseData = new HashMap<>();
        
        try {
            // 현재 로그인된 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(ApiResponse.error("로그인이 필요합니다."));
            }
            
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("사용자를 찾을 수 없습니다."));
            }
            
            // 닉네임 중복 확인
            if (userRepository.existsByNickname(request.getNickname())) {
                return ResponseEntity.ok(ApiResponse.error("이미 사용 중인 닉네임입니다."));
            }
            
            // 닉네임 업데이트
            user.setNickname(request.getNickname());
            userRepository.save(user);
            
            responseData.put("message", "닉네임이 성공적으로 업데이트되었습니다.");
            responseData.put("nickname", request.getNickname());
            responseData.put("email", email);
            
            log.info("닉네임 업데이트 성공: {} -> {}", email, request.getNickname());
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
            
        } catch (Exception e) {
            log.error("닉네임 업데이트 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("닉네임 업데이트 중 오류가 발생했습니다."));
        }
    }

    /**
     * 현재 사용자 정보 조회 API
     */
    @GetMapping("/profile")
    @Operation(
        summary = "사용자 프로필 조회",
        description = "현재 로그인된 사용자의 프로필 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "프로필 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "email": "user@example.com",
                            "nickname": "사용자닉네임",
                            "provider": "google",
                            "createdAt": "2024-01-01T00:00:00"
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile() {
        Map<String, Object> responseData = new HashMap<>();
        
        try {
            // 현재 로그인된 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(ApiResponse.error("로그인이 필요합니다."));
            }
            
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("사용자를 찾을 수 없습니다."));
            }
            
            responseData.put("email", user.getEmail());
            responseData.put("nickname", user.getNickname());
            responseData.put("provider", user.getProvider());
            responseData.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
            
        } catch (Exception e) {
            log.error("프로필 조회 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("프로필 조회 중 오류가 발생했습니다."));
        }
    }


} 