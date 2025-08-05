package com.ssafya408.debatearena.api;

import com.ssafya408.debatearena.common.topic.dto.TopicList;
import com.ssafya408.debatearena.common.topic.dto.TopicListWithTimeResponse;
import com.ssafya408.debatearena.service.topic.DebateTopicService;
import com.ssafya408.debatearena.service.topic.dto.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/topics")
@Tag(name = "토픽 API", description = "토론 주제 조회 관련 API")
@Slf4j
public class TopicController {

    private final DebateTopicService debateTopicService;

    @GetMapping("/current")
    @Operation(
        summary = "현재 토픽 목록 조회",
        description = "Redis에서 현재 활성화된 토픽 목록과 다음 토픽 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토픽 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "topicList": {
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
                            },
                            "timeToNextHour": 1830,
                            "formattedTime": "30분 30초"
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
                          "data": "현재 토픽 목록이 존재하지 않습니다."
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<TopicListWithTimeResponse>> getCurrentTopics() {
        log.info("[토픽 조회] 현재 토픽 목록 조회 요청");
        
        try {
            TopicList topics = debateTopicService.getTopics();
            
            if (topics == null) {
                log.warn("[토픽 조회] 토픽 목록이 존재하지 않음");
                return ResponseEntity.ok(ApiResponse.error("현재 토픽 목록이 존재하지 않습니다."));
            }
            
            // 다음 정시까지 남은 시간 계산
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
            long secondsToNextHour = ChronoUnit.SECONDS.between(now, nextHour);
            
            // 포맷된 시간 생성
            long minutes = secondsToNextHour / 60;
            long seconds = secondsToNextHour % 60;
            String formattedTime = String.format("%d분 %d초", minutes, seconds);
            
            // ResponseDto 생성
            TopicListWithTimeResponse response = TopicListWithTimeResponse.builder()
                .topicList(topics)
                .timeToNextHour(secondsToNextHour)
                .formattedTime(formattedTime)
                .build();
            
            log.info("[토픽 조회] 성공 - 현재 토픽: {}, 다음 토픽: {}", 
                topics.getCurrentTopics().size(), topics.getNextTopics().size());
            log.info("[토픽 조회] 다음 정시까지 남은 시간: {}초 ({})", secondsToNextHour, formattedTime);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("[토픽 조회] 실패 - 오류: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("토픽 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}