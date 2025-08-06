package com.ssafya408.debate.domain.api;

import com.ssafya408.debate.domain.api.dto.room.DebateParticipantRequest;
import com.ssafya408.debate.domain.api.dto.room.DebateRoomResponse;
import com.ssafya408.debate.domain.api.service.DebateService;
import com.ssafya408.debate.domain.common.dto.ApiResponse;
import com.ssafya408.debate.domain.db.Topic;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
@Tag(name = "토론방 API", description = "토론방 생성 및 관리 관련 API")
@Slf4j
public class DebateRoomApiController {

  private final DebateService debateService;

  @GetMapping("/test")
  @Operation(
    summary = "CORS 테스트",
    description = "CORS 설정이 올바르게 작동하는지 테스트합니다."
  )
  @ApiResponses(value = {
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "테스트 성공",
      content = @Content(
        mediaType = "application/json",
        examples = @ExampleObject(
          name = "성공 예시",
          value = """
            {
              "status": "success",
              "data": {
                "message": "CORS 테스트 성공!",
                "service": "debate",
                "timestamp": "2024-01-01T12:00:00"
              }
            }
            """
        )
      )
    )
  })
  public ResponseEntity<ApiResponse<Map<String, String>>> testCors() {
    log.info("[CORS 테스트] 요청 수신");
    
    Map<String, String> response = new HashMap<>();
    response.put("message", "CORS 테스트 성공!");
    response.put("service", "debate");
    response.put("timestamp", java.time.LocalDateTime.now().toString());
    
    log.info("[CORS 테스트] 응답: {}", response);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping("/topics")
  @Operation(
    summary = "사용 가능한 토론 주제 목록 조회",
    description = "토론방 생성에 사용할 수 있는 주제 목록을 조회합니다."
  )
  @ApiResponses(value = {
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "주제 목록 조회 성공",
      content = @Content(
        mediaType = "application/json",
        examples = @ExampleObject(
          name = "성공 예시",
          value = """
            {
              "status": "success",
              "data": [
                {
                  "id": 1,
                  "topicText": "인공지능의 윤리적 문제",
                  "firstOption": "인공지능 개발을 제한해야 한다",
                  "secondOption": "인공지능 개발을 적극적으로 지원해야 한다"
                }
              ]
            }
            """
        )
      )
    )
  })
  public ResponseEntity<ApiResponse<List<Topic>>> getAvailableTopics() {
    log.info("[토론 주제 목록 조회] 요청 수신");
    
    List<Topic> topics = debateService.getAvailableTopics();
    
    log.info("[토론 주제 목록 조회] 조회된 주제 개수: {}", topics.size());
    topics.forEach(topic -> 
      log.info("[토론 주제 목록 조회] 주제: ID={}, 제목={}", topic.getId(), topic.getTopicText())
    );
    
    return ResponseEntity.ok(ApiResponse.success(topics));
  }

  @PostMapping("")
  @Operation(
    summary = "토론방 생성",
    description = "새로운 토론방을 생성합니다."
  )
  @ApiResponses(value = {
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "토론방 생성 성공",
      content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiResponse.class),
        examples = @ExampleObject(
          name = "성공 예시",
          value = """
            {
              "status": "success",
              "data": {
                "roomId": "123"
              }
            }
            """
        )
      )
    )
  })
  public ResponseEntity<ApiResponse<DebateRoomResponse>> createDebateRoom(
      @RequestBody @Schema(description = "토론방 생성 요청", implementation = DebateParticipantRequest.class)
      DebateParticipantRequest req
  ) {
    log.info("[토론방 생성] 요청 수신");
    log.info("[토론방 생성] 요청 데이터: matchId={}, topicId={}, matchType={}", 
        req.getMatchId(), req.getTopicId(), req.getMatchType());
    log.info("[토론방 생성] 첫 번째 팀: {}", req.getFirstTeam());
    log.info("[토론방 생성] 두 번째 팀: {}", req.getSecondTeam());

    try {
      Long roomId = debateService.generateDebateRoom(req);

      
      log.info("[토론방 생성] 성공 - 생성된 방 ID: {}", roomId);
      return ResponseEntity.ok(ApiResponse.success(DebateRoomResponse.builder().roomId(roomId).build()));
      
    } catch (Exception e) {
      log.error("[토론방 생성] 실패 - 오류: {}", e.getMessage(), e);
      Map<String, String> errorRes = new HashMap<>();
      errorRes.put("error", "토론방 생성에 실패했습니다: " + e.getMessage());
      return ResponseEntity.ok(ApiResponse.error("토론방 생성에 실패했습니다: " + e.getMessage()));
    }
  }

  @GetMapping("/createDebate")
  @Operation(
          summary = "토론 시작",
          description = "토론을 시작합니다."
  )
  public void createDebateRoom(@RequestParam String userEmail, @RequestParam Long roomId) {
    try {

      log.info("[토론 진행 시작]");

      debateService.userJoinMatch(userEmail, roomId);

      log.info("[토론 진행] 성공 ");

    } catch (Exception e) {
      log.error("[토론 진행] 실패 - 오류: {}", e.getMessage(), e);

    }
  }

}
