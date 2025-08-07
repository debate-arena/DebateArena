package com.ssafya408.matching.api;

import com.ssafya408.matching.common.topic.dto.TopicList;
import com.ssafya408.matching.common.topic.service.TopicService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Matching 서비스의 테스트 API 컨트롤러
 * refreshTopicsAndMatchQueue 기능을 테스트하기 위한 API 제공
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matching/test")
@Tag(name = "매칭 테스트 API", description = "매칭 서비스 테스트 및 관리 관련 API")
@Slf4j
public class MatchTestController {

    private final MatchService matchService;
    private final TopicService topicService;

    @PostMapping("/refresh-topics")
    @Operation(
        summary = "토픽 및 매칭 큐 수동 갱신 (테스트용)",
        description = "스케줄러를 기다리지 않고 수동으로 토픽 정보를 갱신하고 매칭 큐를 리셋합니다. refreshTopicsAndMatchQueue() 메서드를 직접 호출합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토픽 갱신 및 큐 리셋 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "message": "토픽 갱신 및 매칭 큐 리셋이 성공적으로 완료되었습니다.",
                          "timestamp": "2024-01-01T12:00:00",
                          "beforeTopics": [1, 2, 3, 4, 5],
                          "afterTopics": [6, 7, 8, 9, 10],
                          "queueReset": true
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> refreshTopicsAndMatchQueue() {
        log.info("[테스트 API] refreshTopicsAndMatchQueue 수동 실행 요청");
        
        try {
            // 갱신 전 토픽 상태 저장
            List<Long> beforeTopics = matchService.getTopicIdxToId();
            TopicList beforeTopicList = topicService.getTopics();
            
            // 토픽 갱신 및 매칭 큐 리셋 실행
            matchService.refreshTopicsAndMatchQueue();
            
            // 갱신 후 토픽 상태 조회
            List<Long> afterTopics = matchService.getTopicIdxToId();
            TopicList afterTopicList = topicService.getTopics();
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "토픽 갱신 및 매칭 큐 리셋이 성공적으로 완료되었습니다.");
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("beforeTopics", beforeTopics);
            result.put("afterTopics", afterTopics);
            result.put("queueReset", true);
            
            // 상세 토픽 정보 포함
            if (beforeTopicList != null) {
                result.put("beforeTopicDetails", beforeTopicList.getCurrentTopics());
            }
            if (afterTopicList != null) {
                result.put("afterTopicDetails", afterTopicList.getCurrentTopics());
            }
            
            log.info("[테스트 API] refreshTopicsAndMatchQueue 실행 완료 - 이전: {}, 이후: {}", 
                beforeTopics, afterTopics);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("[테스트 API] refreshTopicsAndMatchQueue 실행 실패 - 오류: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "토픽 갱신 중 오류가 발생했습니다: " + e.getMessage());
            result.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/topics/current")
    @Operation(
        summary = "현재 매칭 서비스의 토픽 상태 조회",
        description = "매칭 서비스에서 현재 사용 중인 토픽 정보와 큐 매핑 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토픽 상태 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "queueTopicIds": [1, 2, 3, 4, 5],
                            "redisTopics": {
                              "currentTopics": [
                                {
                                  "id": 1,
                                  "topicText": "인공지능의 윤리적 문제",
                                  "firstOption": "개발 제한",
                                  "secondOption": "개발 지원"
                                }
                              ]
                            },
                            "timestamp": "2024-01-01T12:00:00"
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getCurrentTopicStatus() {
        log.info("[테스트 API] 현재 토픽 상태 조회 요청");
        
        try {
            List<Long> queueTopics = matchService.getTopicIdxToId();
            TopicList redisTopics = topicService.getTopics();
            
            Map<String, Object> data = new HashMap<>();
            data.put("queueTopicIds", queueTopics);
            data.put("redisTopics", redisTopics);
            data.put("timestamp", LocalDateTime.now().toString());
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("data", data);
            
            log.info("[테스트 API] 토픽 상태 조회 완료 - 큐 토픽: {}", queueTopics);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("[테스트 API] 토픽 상태 조회 실패 - 오류: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "토픽 상태 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/queue/status")
    @Operation(
        summary = "매칭 큐 상태 조회",
        description = "현재 매칭 큐의 대기자 수와 상태를 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "큐 상태 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "message": "매칭 큐 상태 조회가 완료되었습니다.",
                          "timestamp": "2024-01-01T12:00:00"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getMatchQueueStatus() {
        log.info("[테스트 API] 매칭 큐 상태 조회 요청");
        
        try {
            // 매칭 상태 전송 (기존 로직 재사용)
            matchService.sendMatchStatus();
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "매칭 큐 상태 조회가 완료되었습니다. WebSocket(/sub/match/status)로 상태가 전송되었습니다.");
            result.put("timestamp", LocalDateTime.now().toString());
            
            log.info("[테스트 API] 매칭 큐 상태 전송 완료");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("[테스트 API] 매칭 큐 상태 조회 실패 - 오류: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "매칭 큐 상태 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/queue/reset")
    @Operation(
        summary = "매칭 큐 수동 리셋 (테스트용)",
        description = "현재 매칭 큐를 강제로 리셋합니다. 큐에 있는 모든 사용자들에게 취소 알림이 전송됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "큐 리셋 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "message": "매칭 큐가 성공적으로 리셋되었습니다.",
                          "timestamp": "2024-01-01T12:00:00"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> resetMatchQueue() {
        log.info("[테스트 API] 매칭 큐 수동 리셋 요청");
        
        try {
            // refreshTopicsAndMatchQueue의 큐 리셋 부분만 실행
            matchService.refreshTopicsAndMatchQueue();
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "매칭 큐가 성공적으로 리셋되었습니다.");
            result.put("timestamp", LocalDateTime.now().toString());
            
            log.info("[테스트 API] 매칭 큐 리셋 완료");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("[테스트 API] 매칭 큐 리셋 실패 - 오류: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "매칭 큐 리셋 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/clear-already-matched")
    @Operation(
        summary = "이미 매칭된 사용자 목록 초기화 (테스트용)",
        description = "alreadyMatched 세트를 초기화하여 모든 사용자가 다시 매칭에 참여할 수 있도록 합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "alreadyMatched 초기화 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "message": "alreadyMatched가 성공적으로 초기화되었습니다.",
                          "beforeCount": 3,
                          "afterCount": 0,
                          "timestamp": "2024-01-20T10:30:00"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> clearAlreadyMatched() {
        log.info("[테스트 API] alreadyMatched 초기화 요청");
        
        try {
            int beforeCount = matchService.getAlreadyMatchedCount();
            matchService.clearAlreadyMatched();
            int afterCount = matchService.getAlreadyMatchedCount();
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "alreadyMatched가 성공적으로 초기화되었습니다.");
            result.put("beforeCount", beforeCount);
            result.put("afterCount", afterCount);
            result.put("timestamp", LocalDateTime.now().toString());
            
            log.info("[테스트 API] alreadyMatched 초기화 완료 - 이전: {}명, 이후: {}명", beforeCount, afterCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("[테스트 API] alreadyMatched 초기화 실패 - 오류: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "alreadyMatched 초기화 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/already-matched/status")
    @Operation(
        summary = "이미 매칭된 사용자 목록 조회 (테스트용)",
        description = "현재 alreadyMatched에 포함된 사용자 목록과 개수를 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "alreadyMatched 상태 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "count": 2,
                          "users": ["user1", "user2"],
                          "timestamp": "2024-01-20T10:30:00"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getAlreadyMatchedStatus() {
        log.info("[테스트 API] alreadyMatched 상태 조회 요청");
        
        try {
            int count = matchService.getAlreadyMatchedCount();
            var users = matchService.getAlreadyMatchedUsers();
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("count", count);
            result.put("users", users);
            result.put("timestamp", LocalDateTime.now().toString());
            
            log.info("[테스트 API] alreadyMatched 상태 조회 완료 - {}명: {}", count, users);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("[테스트 API] alreadyMatched 상태 조회 실패 - 오류: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "alreadyMatched 상태 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}