package com.ssafya408.debatearena.api;

import com.ssafya408.debatearena.common.topic.dto.TopicList;
import com.ssafya408.debatearena.service.topic.DebateTopicService;
import com.ssafya408.debatearena.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/topics/test")
@Tag(name = "토픽 테스트 API", description = "토론 주제 테스트 및 관리 관련 API")
@Slf4j
public class TopicTestController {

    private final DebateTopicService debateTopicService;

    @PostMapping("/modify")
    @Operation(
        summary = "토픽 수동 변경 (테스트용)",
        description = "스케줄러를 기다리지 않고 수동으로 토픽을 변경합니다. modifyTopics() 메서드를 직접 호출합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토픽 변경 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "message": "토픽이 성공적으로 변경되었습니다.",
                            "timestamp": "2024-01-01T12:00:00",
                            "beforeCurrentTopics": [
                              {
                                "id": 1,
                                "topicText": "인공지능의 윤리적 문제",
                                "firstOption": "인공지능 개발을 제한해야 한다",
                                "secondOption": "인공지능 개발을 적극적으로 지원해야 한다"
                              }
                            ],
                            "afterCurrentTopics": [
                              {
                                "id": 2,
                                "topicText": "온라인 교육의 효과",
                                "firstOption": "온라인 교육이 더 효과적이다",
                                "secondOption": "오프라인 교육이 더 효과적이다"
                              }
                            ],
                            "afterNextTopics": [
                              {
                                "id": 3,
                                "topicText": "환경보호와 경제발전",
                                "firstOption": "환경보호가 우선되어야 한다",
                                "secondOption": "경제발전이 우선되어야 한다"
                              }
                            ]
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> modifyTopicsManually() {
        log.info("[토픽 변경] 수동 토픽 변경 요청");
        
        try {
            // 변경 전 상태 저장
            TopicList beforeTopics = debateTopicService.getTopics();
            
            // 토픽 변경 실행
            debateTopicService.modifyTopics();
            
            // 변경 후 상태 조회
            TopicList afterTopics = debateTopicService.getTopics();
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "토픽이 성공적으로 변경되었습니다.");
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            
            if (beforeTopics != null) {
                result.put("beforeCurrentTopics", beforeTopics.getCurrentTopics());
                result.put("beforeNextTopics", beforeTopics.getNextTopics());
            } else {
                result.put("beforeCurrentTopics", "없음");
                result.put("beforeNextTopics", "없음");
            }
            
            if (afterTopics != null) {
                result.put("afterCurrentTopics", afterTopics.getCurrentTopics());
                result.put("afterNextTopics", afterTopics.getNextTopics());
            } else {
                result.put("afterCurrentTopics", "없음");
                result.put("afterNextTopics", "없음");
            }
            
            log.info("[토픽 변경] 성공 - 변경 전: {}, 변경 후: {}", 
                beforeTopics != null ? beforeTopics.getCurrentTopics().size() : "없음",
                afterTopics != null ? afterTopics.getCurrentTopics().size() : "없음");
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("[토픽 변경] 실패 - 오류: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("토픽 변경 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/initiate")
    @Operation(
        summary = "토픽 초기화 (테스트용)",
        description = "Redis에 초기 토픽 목록을 설정합니다. initiateTopics() 메서드를 직접 호출합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토픽 초기화 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "message": "토픽이 성공적으로 초기화되었습니다.",
                            "timestamp": "2024-01-01T12:00:00",
                            "currentTopics": [
                              {
                                "id": 1,
                                "topicText": "인공지능의 윤리적 문제",
                                "firstOption": "인공지능 개발을 제한해야 한다",
                                "secondOption": "인공지능 개발을 적극적으로 지원해야 한다"
                              }
                            ],
                            "nextTopics": [
                              {
                                "id": 2,
                                "topicText": "온라인 교육의 효과",
                                "firstOption": "온라인 교육이 더 효과적이다",
                                "secondOption": "오프라인 교육이 더 효과적이다"
                              }
                            ]
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateTopics() {
        log.info("[토픽 초기화] 토픽 초기화 요청");
        
        try {
            // 토픽 초기화 실행
            debateTopicService.initiateTopics();
            
            // 초기화 후 상태 조회
            TopicList topics = debateTopicService.getTopics();
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "토픽이 성공적으로 초기화되었습니다.");
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            
            if (topics != null) {
                result.put("currentTopics", topics.getCurrentTopics());
                result.put("nextTopics", topics.getNextTopics());
            } else {
                result.put("currentTopics", "초기화 실패");
                result.put("nextTopics", "초기화 실패");
            }
            
            log.info("[토픽 초기화] 성공 - 현재 토픽: {}, 다음 토픽: {}", 
                topics != null ? topics.getCurrentTopics().size() : "없음",
                topics != null ? topics.getNextTopics().size() : "없음");
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("[토픽 초기화] 실패 - 오류: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("토픽 초기화 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    @Operation(
        summary = "Redis 토픽 데이터 삭제 (테스트용)",
        description = "Redis에서 토픽 관련 데이터를 모두 삭제합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토픽 데이터 삭제 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "message": "Redis 토픽 데이터가 성공적으로 삭제되었습니다.",
                            "timestamp": "2024-01-01T12:00:00"
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> clearTopics() {
        log.info("[토픽 삭제] Redis 토픽 데이터 삭제 요청");
        
        try {
            // Redis에서 토픽 데이터 삭제
            debateTopicService.clearTopics();
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Redis 토픽 데이터가 성공적으로 삭제되었습니다.");
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            
            log.info("[토픽 삭제] 성공");
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("[토픽 삭제] 실패 - 오류: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("토픽 데이터 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}