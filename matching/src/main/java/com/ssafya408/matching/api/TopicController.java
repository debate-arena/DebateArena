package com.ssafya408.matching.api;

import com.ssafya408.matching.common.dto.ApiResponse;
import com.ssafya408.matching.common.topic.dto.TopicDto;
import com.ssafya408.matching.common.topic.dto.TopicList;
import com.ssafya408.matching.common.topic.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Topic Management", description = "토픽 관리 API")
public class TopicController {
    
    private final TopicService topicService;
    
    @Operation(summary = "현재 토픽 목록 조회", description = "Redis에서 현재 활성화된 토픽 목록을 가져옵니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토픽 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "토픽 데이터 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<?>> getCurrentTopics() {
        try {
            log.info("[API] 현재 토픽 목록 조회 요청");
            
            TopicList topicList = topicService.getTopics();
            
            if (topicList == null || topicList.getCurrentTopics() == null) {
                log.warn("[API] Redis에서 토픽 데이터를 찾을 수 없습니다");
                return ResponseEntity.ok(ApiResponse.fail("토픽 데이터를 찾을 수 없습니다"));
            }
            
            List<TopicDto> currentTopics = topicList.getCurrentTopics();
            log.info("[API] 현재 토픽 조회 완료: {} 개", currentTopics.size());
            
            return ResponseEntity.ok(ApiResponse.success(currentTopics));
            
        } catch (Exception e) {
            log.error("[API] 현재 토픽 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("토픽 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "전체 토픽 정보 조회", description = "Redis에서 현재 및 다음 토픽 정보를 모두 가져옵니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토픽 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "토픽 데이터 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getAllTopics() {
        try {
            log.info("[API] 전체 토픽 정보 조회 요청");
            
            TopicList topicList = topicService.getTopics();
            
            if (topicList == null) {
                log.warn("[API] Redis에서 토픽 데이터를 찾을 수 없습니다");
                return ResponseEntity.ok(ApiResponse.fail("토픽 데이터를 찾을 수 없습니다"));
            }
            
            log.info("[API] 전체 토픽 조회 완료 - 현재: {} 개, 다음: {} 개", 
                topicList.getCurrentTopics() != null ? topicList.getCurrentTopics().size() : 0,
                topicList.getNextTopics() != null ? topicList.getNextTopics().size() : 0);
            
            return ResponseEntity.ok(ApiResponse.success(topicList));
            
        } catch (Exception e) {
            log.error("[API] 전체 토픽 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("토픽 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "토픽 새로고침", description = "Redis에서 토픽 정보를 다시 로드합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토픽 새로고침 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshTopics() {
        try {
            log.info("[API] 토픽 새로고침 요청");
            
            // TopicService를 통해 최신 데이터 조회
            TopicList topicList = topicService.getTopics();
            
            if (topicList == null) {
                log.warn("[API] 새로고침 후에도 토픽 데이터를 찾을 수 없습니다");
                return ResponseEntity.ok(ApiResponse.fail("토픽 데이터를 찾을 수 없습니다"));
            }
            
            log.info("[API] 토픽 새로고침 완료");
            return ResponseEntity.ok(ApiResponse.success("토픽 정보가 성공적으로 새로고침되었습니다"));
            
        } catch (Exception e) {
            log.error("[API] 토픽 새로고침 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("토픽 새로고침 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
