package com.ssafya408.matching.api;

import com.ssafya408.matching.common.topic.dto.TopicList;
import com.ssafya408.matching.common.topic.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Matching 서비스에서 토픽 정보를 조회하는 컨트롤러
 * debate-arena에서 설정한 토픽 정보를 재사용
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matching/topics")
@Tag(name = "매칭 토픽 API", description = "매칭 서비스에서 토론 주제 조회 관련 API")
@Slf4j
public class TopicController {

    private final TopicService topicService;

    @GetMapping("/current")
    @Operation(
        summary = "매칭용 현재 토픽 목록 조회",
        description = "Redis에서 현재 활성화된 토픽 목록을 조회합니다. debate-arena에서 설정한 토픽을 재사용합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토픽 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
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
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "토픽 목록이 존재하지 않음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "실패 예시",
                    value = """
                        {
                          "status": "error",
                          "message": "현재 토픽 목록이 존재하지 않습니다."
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getCurrentTopics() {
        log.info("[매칭 토픽 조회] 현재 토픽 목록 조회 요청");
        
        try {
            TopicList topics = topicService.getTopics();
            
            Map<String, Object> response = new HashMap<>();
            
            if (topics == null) {
                log.warn("[매칭 토픽 조회] 토픽 목록이 존재하지 않음");
                response.put("status", "error");
                response.put("message", "현재 토픽 목록이 존재하지 않습니다.");
                return ResponseEntity.ok(response);
            }
            
            log.info("[매칭 토픽 조회] 성공 - 현재 토픽: {}, 다음 토픽: {}", 
                topics.getCurrentTopics().size(), topics.getNextTopics().size());
            
            response.put("status", "success");
            response.put("data", topics);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[매칭 토픽 조회] 실패 - 오류: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "토픽 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}