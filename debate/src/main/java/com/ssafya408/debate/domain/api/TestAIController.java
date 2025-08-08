package com.ssafya408.debate.domain.api;

import com.ssafya408.debate.domain.api.dto.stt.ai.*;
import com.ssafya408.debate.domain.api.service.AiService;
import com.ssafya408.debate.domain.api.service.DebateService;
import com.ssafya408.debate.domain.common.dto.ApiResponse;
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

@RestController
@RequestMapping("/api/ai-test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI 서버 테스트 API", description = "AI 서버와의 통신을 테스트하는 API들")
public class TestAIController {

    private final DebateService debateService;
    private final TopicRepository topicRepository;
    private final AiService aiService;

    /**
     * AI 서버 의견 요약 기능 테스트 (주요 API - 유지)
     * route.py의 /summaries/opinion API와 연동
     */
    @PostMapping("/opinion-summary")
    @Operation(
        summary = "AI 서버 의견 요약 테스트",
        description = "토론에서 발표한 의견을 AI 서버에 전송하여 요약을 받아옵니다. route.py의 /summaries/opinion API와 연동됩니다."
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
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "AI 서버 통신 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "실패 예시",
                    value = """
                        {
                          "status": "error",
                          "data": "의견 요약 요청 실패: Connection refused"
                        }
                        """
                )
            )
        )
    })
    public ApiResponse<OpinionSummaryResponse> testOpinionSummary(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "의견 요약 요청 데이터",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = OpinionTextRequest.class),
                    examples = @ExampleObject(
                        name = "요청 예시",
                        value = """
                            {
                              "user_id": "user123",
                              "topic": "야근 vs 출근길 2시간",
                              "position": "야근",
                              "text": "야근을 선택하겠습니다. 차라리 늦게까지 일하고 집에서 푹 쉬는 게 출근길에 오래 고생하는 것보다 낫다고 생각해요."
                            }
                            """
                    )
                )
            )
            @RequestBody OpinionTextRequest request) {
        
        log.info("=== AI 서버 의견 요약 테스트 시작 ===");
        log.info("요청 데이터 - userId: {}, topic: {}, position: {}", 
            request.getUser_id(), request.getTopic(), request.getPosition());

        try {
            OpinionSummaryResponse response = aiService.requestOpinionSummary(request);
            log.info("AI 서버 의견 요약 테스트 성공 - userId: {}", request.getUser_id());
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("AI 서버 의견 요약 테스트 실패 - userId: {}, error: {}", 
                request.getUser_id(), e.getMessage(), e);
            log.error("요청 데이터: {}", request);
            return ApiResponse.error("의견 요약 요청 실패: " + e.getMessage());
        }
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
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "AI 서버 통신 실패"
        )
    })
    public ApiResponse<SiegeDefenseResponse> testSiegeDefenseSummary(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "공방전 요약 요청 데이터",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = SiegeDefenseRequest.class),
                    examples = @ExampleObject(
                        name = "요청 예시",
                        value = """
                            {
                              "topic": "호랑이와 사자 중 누가 동물의 왕인가",
                              "key": {
                                "attack": {
                                  "user_id": "abc123",
                                  "position": "호랑이가 이긴다",
                                  "text": "욜로 욜로 하다가 골로 간다는 말이 있다..."
                                },
                                "defense": {
                                  "user_id": "abc321",
                                  "position": "사자가 왕이다.",
                                  "text": "사자는 소비를 함으로써 경제를 순환시킨다..."
                                }
                              }
                            }
                            """
                    )
                )
            )
            @RequestBody SiegeDefenseRequest request) {
        
        log.info("=== AI 서버 공방전 요약 테스트 시작 ===");
        log.info("요청 데이터 - topic: {}", request.getTopic());

        try {
            SiegeDefenseResponse response = aiService.requestSiegeDefenseSummary(request);
            log.info("AI 서버 공방전 요약 테스트 성공");
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("AI 서버 공방전 요약 테스트 실패 - error: {}", e.getMessage(), e);
            log.error("요청 데이터: {}", request);
            return ApiResponse.error("공방전 요약 요청 실패: " + e.getMessage());
        }
    }

    /**
     * AI 서버 토론 최종 결과 요청 테스트
     * route.py의 /summaries/result API와 연동
     */
    @PostMapping("/debate-result")
    @Operation(
        summary = "AI 서버 토론 최종 결과 테스트",
        description = "전체 토론 내용을 AI 서버에 전송하여 최종 판정 및 요약을 받아옵니다. route.py의 /summaries/result API와 연동됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토론 결과 요약 성공",
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
                              },
                              "details": [
                                {
                                  "juror": 0,
                                  "vote": "num1",
                                  "sim1": 0.10478,
                                  "sim2": 0.10152,
                                  "diff": 0.00326
                                },
                                {
                                  "juror": 1,
                                  "vote": "num2",
                                  "sim1": 0.10581,
                                  "sim2": 0.12208,
                                  "diff": -0.01627
                                },
                                "... (총 50명의 판정단 데이터)"
                              ]
                            },
                            "juror_explain": "대표 청중 #44의 선택 이유:\\n실전 무술에 관심 많은 30대 남성입니다. 단검의 빠른 공격 속도와 치명타 능력이 현실적인 생존 상황에서 훨씬 유리하다고 느꼈어요. 빠따는 위력이 크지만 준비 동작과 체력 소모가 커서 긴박한 싸움에선 단검이 더 효율적일 것 같습니다.",
                            "full_summarize": {
                              "num1": "단도 진영의 주장은 단검이 빠따에 비해 실전 상황에서 더 우위에 있다는 논리적 근거에 집중되어 있다...",
                              "num2": "야구 빠따가 단도에 비해 우위에 있다는 주장의 핵심 논리는 리치, 즉 공격 거리의 차이에 기반한다..."
                            }
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "AI 서버 통신 실패"
        )
    })
    public ApiResponse<DebateResultResponse> testDebateResult(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "토론 최종 결과 요청 데이터",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = DebateResultRequest.class),
                    examples = @ExampleObject(
                        name = "요청 예시",
                        value = """
                            {
                              "topic": "야구 빠따 vs 단도",
                              "draw": true,
                              "entire": {
                                "num1": {
                                  "position": "단도가 이긴다",
                                  "text": "단도 진영은 빠따에 비해 훨씬 빠른 공격 속도...",
                                  "rebuttal_score": 4.2
                                },
                                "num2": {
                                  "position": "야구 빠따가 이긴다",
                                  "text": "야구빠따가 단도에 비해 월등히 긴 리치...",
                                  "rebuttal_score": 3.5
                                }
                              }
                            }
                            """
                    )
                )
            )
            @RequestBody DebateResultRequest request) {
        
        log.info("=== AI 서버 토론 최종 결과 테스트 시작 ===");
        log.info("요청 데이터 - topic: {}, draw: {}", request.getTopic(), request.getDraw());

        try {
            DebateResultResponse response = aiService.requestDebateResult(request);
            log.info("AI 서버 토론 최종 결과 테스트 성공");
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("AI 서버 토론 최종 결과 테스트 실패 - error: {}", e.getMessage(), e);
            log.error("요청 데이터: {}", request);
            return ApiResponse.error("토론 결과 요청 실패: " + e.getMessage());
        }
    }

    /**
     * 사용 가능한 토론 주제 목록 조회 (유틸리티 API)
     */
    @GetMapping("/topics")
    @Operation(
        summary = "토론 주제 목록 조회",
        description = "테스트에 사용할 수 있는 토론 주제 목록을 조회합니다."
    )
    public ApiResponse<List<Topic>> getTopics() {
        log.info("=== 토론 주제 목록 조회 테스트 ===");
        
        try {
            List<Topic> topics = debateService.getAvailableTopics();
            log.info("토론 주제 목록 조회 성공 - 총 {}개", topics.size());
            return ApiResponse.success(topics);
        } catch (Exception e) {
            log.error("토론 주제 목록 조회 실패 - error: {}", e.getMessage(), e);
            return ApiResponse.error("토론 주제 목록 조회 실패: " + e.getMessage());
        }
    }
}
