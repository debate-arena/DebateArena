package com.ssafya408.debate.domain.api;

import com.ssafya408.debate.domain.api.dto.ai.DebateResultRequest;
import com.ssafya408.debate.domain.api.dto.ai.DebateResultResponse;
import com.ssafya408.debate.domain.api.dto.ai.OpinionSummaryResponse;
import com.ssafya408.debate.domain.api.dto.ai.OpinionTextRequest;
import com.ssafya408.debate.domain.api.dto.ai.SiegeDefenseRequest;
import com.ssafya408.debate.domain.api.dto.ai.SiegeDefenseResponse;
import com.ssafya408.debate.domain.api.dto.summary.DebateSummaryResponse;
import com.ssafya408.debate.domain.api.service.AiService;
import com.ssafya408.debate.domain.api.service.DebateService;
import com.ssafya408.debate.domain.common.dto.ApiResponse;
import com.ssafya408.debate.domain.db.cache.SummaryRedisRepository;
import com.ssafya408.debate.domain.db.rdb.Topic;
import com.ssafya408.debate.domain.db.rdb.TopicRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/test/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI 서버 테스트", description = "AI 서버 연동 기능을 테스트하는 API")
public class TestAIController {

    private final AiService aiService;
    private final DebateService debateService;
    private final TopicRepository topicRepository;
    private final SummaryRedisRepository summaryRedisRepository;

    /**
     * AI 서버 의견 요약 기능 테스트
     * route.py의 /summaries/opinion API와 연동
     */
    @PostMapping("/opinion-summary")
    @Operation(
        summary = "AI 서버 의견 요약 테스트",
        description = "토론 참가자의 의견을 AI 서버에 전송하여 요약을 받아옵니다. route.py의 /summaries/opinion API와 연동됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "의견 요약 성공",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "result": {
                              "user_id": "user123",
                              "text": "야근하며 늦게까지 일하고 집에서 쉬는 것이 출근길 2시간 고생하는 것보다 낫다고 생각한다."
                            }
                          }
                        }
                        """
                )
            )
        )
    })
    public Mono<ApiResponse<String>> testOpinionSummary(
            @RequestBody OpinionTextRequest request,
            @RequestParam(defaultValue = "1") Long roomId) {
        
        log.info("=== AI 서버 의견 요약 테스트 시작 ===");
        log.info("요청 데이터 - userId: {}, topic: {}, roomId: {}", 
            request.getUser_id(), request.getTopic(), roomId);

        return aiService.requestOpinionSummary(roomId, 1, request)
            .then(Mono.fromCallable(() -> {
                log.info("AI 서버 의견 요약 테스트 성공");
                return ApiResponse.success("의견 요약 요청이 성공적으로 처리되었습니다.");
            }))
            .onErrorResume(e -> {
                log.error("AI 서버 의견 요약 테스트 실패 - error: {}", e.getMessage(), e);
                return Mono.just(ApiResponse.error("의견 요약 요청 처리 중 오류가 발생했습니다: " + e.getMessage()));
            });
    }

    /**
     * AI 서버 공방전 요약 기능 테스트
     * route.py의 /summaries/seigedefense API와 연동
     */
    @PostMapping("/siege-defense-summary")
    @Operation(
        summary = "AI 서버 공방전 요약 테스트",
        description = "토론 공방전(공격/방어) 내용을 AI 서버에 전송하여 요약을 받아옵니다. route.py의 /summaries/seigedefense API와 연동됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "공방전 요약 성공",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "result": {
                              "attack_id": "abc123",
                              "defense_id": "abc321",
                              "text": "공격 내용 : 호랑이는 사자보다 과소비로 파산 위험이 크고, 존재 자체가 소비지향적이라 비판받는다.\\n방어 내용 : 사자는 소비로 경제를 활성화시키며 자본주의에서 긍정적 역할을 한다.\\n반박 점수 : 6점\\n방어는 경제 순환 측면에서 반박했으나, 공격의 본질인 과소비 문제를 완전히 해소하지는 못했다.",
                              "rebuttal_score": 6
                            }
                          }
                        }
                        """
                )
            )
        )
    })
    public Mono<ApiResponse<String>> testSiegeDefenseSummary(
            @RequestBody SiegeDefenseRequest request,
            @RequestParam(defaultValue = "1") Long roomId) {
        
        log.info("=== AI 서버 공방전 요약 테스트 시작 ===");
        log.info("요청 데이터 - topic: {}, roomId: {}", request.getTopic(), roomId);

        return aiService.requestSiegeDefenseSummary(roomId, 1, request)
            .then(Mono.fromCallable(() -> {
                log.info("AI 서버 공방전 요약 테스트 성공");
                return ApiResponse.success("공방전 요약 요청이 성공적으로 처리되었습니다.");
            }))
            .onErrorResume(e -> {
                log.error("AI 서버 공방전 요약 테스트 실패 - error: {}", e.getMessage(), e);
                return Mono.just(ApiResponse.error("공방전 요약 요청 처리 중 오류가 발생했습니다: " + e.getMessage()));
            });
    }

    /**
     * AI 서버 토론 최종 결과 기능 테스트
     * route.py의 /summaries/result API와 연동
     */
    @PostMapping("/debate-result")
    @Operation(
        summary = "AI 서버 토론 최종 결과 테스트",
        description = "토론 전체 내용을 AI 서버에 전송하여 최종 결과를 받아옵니다. route.py의 /summaries/result API와 연동됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토론 최종 결과 성공",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "result": {
                              "winner": "num1",
                              "votes": {
                                "num1": 26,
                                "num2": 24,
                                "none": 0
                              },
                              "soft_scores": {
                                "num1": 26.219,
                                "num2": 23.781
                              }
                            },
                            "juror_explain": "대표 청중 #44의 선택 이유...",
                            "full_summarize": {
                              "num1": "단도 진영의 주장은...",
                              "num2": "야구 빠따가 단도에 비해..."
                            }
                          }
                        }
                        """
                )
            )
        )
    })
    public Mono<ApiResponse<String>> testDebateResult(
            @RequestBody DebateResultRequest request,
            @RequestParam(defaultValue = "1") Long roomId) {
        
        log.info("=== AI 서버 토론 최종 결과 테스트 시작 ===");
        log.info("요청 데이터 - topic: {}, draw: {}, roomId: {}", 
            request.getTopic(), request.getDraw(), roomId);

        return aiService.requestDebateResult(roomId, request)
            .then(Mono.fromCallable(() -> {
                log.info("AI 서버 토론 최종 결과 테스트 성공");
                return ApiResponse.success("토론 최종 결과 요청이 성공적으로 처리되었습니다.");
            }))
            .onErrorResume(e -> {
                log.error("AI 서버 토론 최종 결과 테스트 실패 - error: {}", e.getMessage(), e);
                return Mono.just(ApiResponse.error("토론 최종 결과 요청 처리 중 오류가 발생했습니다: " + e.getMessage()));
            });
    }

    /**
     * Redis에 저장된 토론 요약 데이터 조회 테스트
     */
    @GetMapping("/summary/{roomId}")
    @Operation(
        summary = "토론 요약 데이터 조회 테스트",
        description = "Redis에 저장된 특정 방의 의견, 공방전, 최종 요약 데이터를 모두 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요약 데이터 조회 성공",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "roomId": 1,
                            "opinion": [
                              {
                                "phase": "opinion",
                                "round": 1,
                                "user_id": "user123",
                                "text": "야근하며 늦게까지 일하고 집에서 쉬는 것이 출근길 2시간 고생하는 것보다 낫다고 생각한다.",
                                "team": "first",
                                "timestamp": "2024-01-01T12:00:00"
                              }
                            ],
                            "battle": [
                              {
                                "phase": "battle",
                                "round": 1,
                                "attack_id": "abc123",
                                "defense_id": "abc321",
                                "text": "공격 내용 : 호랑이는 사자보다 과소비로 파산 위험이 크고...",
                                "rebuttal_score": 6,
                                "attack_team": "first",
                                "defense_team": "second",
                                "timestamp": "2024-01-01T12:30:00"
                              }
                            ],
                            "final_summary": [],
                            "current_phase": "battle",
                            "current_round": 1,
                            "timestamp": "2024-01-01T13:00:00"
                          }
                        }
                        """
                )
            )
        )
    })
    public ApiResponse<DebateSummaryResponse> getSummaryData(@PathVariable Long roomId) {
        log.info("=== 토론 요약 데이터 조회 테스트 시작 ===");
        log.info("조회 요청 - roomId: {}", roomId);

        try {
            DebateSummaryResponse summaryData = summaryRedisRepository.getAllSummaries(roomId);
            
            log.info("토론 요약 데이터 조회 성공 - roomId: {}, 의견: {}개, 공방전: {}개, 최종: {}개", 
                roomId, 
                summaryData.getOpinion().size(), 
                summaryData.getBattle().size(), 
                summaryData.getFinal_summary().size());
            
            return ApiResponse.success(summaryData);
        } catch (Exception e) {
            log.error("토론 요약 데이터 조회 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
            return ApiResponse.error("요약 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 토론 주제 목록 조회 (테스트용)
     */
    @GetMapping("/topics")
    @Operation(
        summary = "토론 주제 목록 조회",
        description = "테스트용으로 사용할 수 있는 토론 주제 목록을 조회합니다."
    )
    public ApiResponse<List<Topic>> getTopics() {
        log.info("=== 토론 주제 목록 조회 ===");
        
        try {
            List<Topic> topics = topicRepository.findAll();
            log.info("토론 주제 목록 조회 성공 - 총 {}개", topics.size());
            return ApiResponse.success(topics);
        } catch (Exception e) {
            log.error("토론 주제 목록 조회 실패 - error: {}", e.getMessage(), e);
            return ApiResponse.error("토론 주제 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
