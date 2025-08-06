package com.ssafya408.debate.domain.api;

import com.ssafya408.debate.domain.api.dto.room.DebateParticipantRequest;
import com.ssafya408.debate.domain.api.dto.room.DebateRoomResponse;
import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.api.dto.room.WebRTCStatus;
import com.ssafya408.debate.domain.api.service.DebateService;
import com.ssafya408.debate.domain.common.dto.ApiResponse;
import com.ssafya408.debate.domain.db.cache.DebateRedisInfo;
import com.ssafya408.debate.domain.db.rdb.Topic;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

  @GetMapping("/{roomId}/info")
  @Operation(
    summary = "토론방 정보 조회",
    description = "Redis에서 특정 토론방의 상세 정보를 조회합니다."
  )
  public ResponseEntity<ApiResponse<DebateRedisInfo>> getRoomInfo(@PathVariable Long roomId) {
    log.info("[토론방 정보 조회] 요청 수신 - roomId: {}", roomId);
    
    try {
      DebateRedisInfo roomInfo = debateService.getDebateRoomInfo(roomId);
      
      if (roomInfo == null) {
        log.warn("[토론방 정보 조회] 방을 찾을 수 없음 - roomId: {}", roomId);
        return ResponseEntity.ok(ApiResponse.error("토론방을 찾을 수 없습니다"));
      }
      
      log.info("[토론방 정보 조회] 성공 - roomId: {}, status: {}", roomId, roomInfo.getStatus());
      return ResponseEntity.ok(ApiResponse.success(roomInfo));
      
    } catch (Exception e) {
      log.error("[토론방 정보 조회] 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error("토론방 정보 조회에 실패했습니다: " + e.getMessage()));
    }
  }

  @GetMapping("/{roomId}/exists")
  @Operation(
    summary = "토론방 존재 여부 확인",
    description = "Redis에서 토론방의 존재 여부를 확인합니다."
  )
  public ResponseEntity<ApiResponse<Map<String, Object>>> checkRoomExists(@PathVariable Long roomId) {
    log.info("[토론방 존재 여부 확인] 요청 수신 - roomId: {}", roomId);
    
    try {
      boolean exists = debateService.isRoomExists(roomId);
      
      Map<String, Object> response = new HashMap<>();
      response.put("roomId", roomId);
      response.put("exists", exists);
      response.put("timestamp", java.time.LocalDateTime.now().toString());
      
      log.info("[토론방 존재 여부 확인] 성공 - roomId: {}, exists: {}", roomId, exists);
      return ResponseEntity.ok(ApiResponse.success(response));
      
    } catch (Exception e) {
      log.error("[토론방 존재 여부 확인] 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error("존재 여부 확인에 실패했습니다: " + e.getMessage()));
    }
  }

  @GetMapping("/active")
  @Operation(
    summary = "활성 토론방 목록 조회",
    description = "현재 활성화된 모든 토론방의 ID 목록을 조회합니다."
  )
  public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveRooms() {
    log.info("[활성 토론방 목록 조회] 요청 수신");
    
    try {
      java.util.Set<String> activeRoomIds = debateService.getActiveRoomIds();
      
      Map<String, Object> response = new HashMap<>();
      response.put("activeRooms", activeRoomIds);
      response.put("count", activeRoomIds.size());
      response.put("timestamp", java.time.LocalDateTime.now().toString());
      
      log.info("[활성 토론방 목록 조회] 성공 - 활성 방 개수: {}", activeRoomIds.size());
      return ResponseEntity.ok(ApiResponse.success(response));
      
    } catch (Exception e) {
      log.error("[활성 토론방 목록 조회] 실패 - error: {}", e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error("활성 토론방 목록 조회에 실패했습니다: " + e.getMessage()));
    }
  }

  @PutMapping("/{roomId}/status")
  @Operation(
    summary = "토론방 상태 업데이트",
    description = "토론방의 상태를 업데이트합니다."
  )
  public ResponseEntity<ApiResponse<Map<String, Object>>> updateRoomStatus(
      @PathVariable Long roomId,
      @RequestParam RoomStatus status
  ) {
    log.info("[토론방 상태 업데이트] 요청 수신 - roomId: {}, status: {}", roomId, status);
    
    try {
      debateService.updateRoomStatus(roomId, status);
      
      Map<String, Object> response = new HashMap<>();
      response.put("roomId", roomId);
      response.put("status", status);
      response.put("timestamp", java.time.LocalDateTime.now().toString());
      
      log.info("[토론방 상태 업데이트] 성공 - roomId: {}, status: {}", roomId, status);
      return ResponseEntity.ok(ApiResponse.success(response));
      
    } catch (Exception e) {
      log.error("[토론방 상태 업데이트] 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error("상태 업데이트에 실패했습니다: " + e.getMessage()));
    }
  }

  @PutMapping("/{roomId}/webrtc-status")
  @Operation(
    summary = "WebRTC 상태 업데이트",
    description = "토론방의 WebRTC 상태를 업데이트합니다."
  )
  public ResponseEntity<ApiResponse<Map<String, Object>>> updateWebRTCStatus(
      @PathVariable Long roomId,
      @RequestParam WebRTCStatus status
  ) {
    log.info("[WebRTC 상태 업데이트] 요청 수신 - roomId: {}, status: {}", roomId, status);
    
    try {
      debateService.updateWebRTCStatus(roomId, status);
      
      Map<String, Object> response = new HashMap<>();
      response.put("roomId", roomId);
      response.put("webRTCStatus", status);
      response.put("timestamp", java.time.LocalDateTime.now().toString());
      
      log.info("[WebRTC 상태 업데이트] 성공 - roomId: {}, status: {}", roomId, status);
      return ResponseEntity.ok(ApiResponse.success(response));
      
    } catch (Exception e) {
      log.error("[WebRTC 상태 업데이트] 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error("WebRTC 상태 업데이트에 실패했습니다: " + e.getMessage()));
    }
  }

  @PostMapping("/{roomId}/extend-ttl")
  @Operation(
    summary = "토론방 TTL 연장",
    description = "진행 중인 토론방의 TTL을 연장합니다."
  )
  public ResponseEntity<ApiResponse<Map<String, Object>>> extendRoomTTL(@PathVariable Long roomId) {
    log.info("[토론방 TTL 연장] 요청 수신 - roomId: {}", roomId);
    
    try {
      debateService.extendRoomTTL(roomId);
      
      Map<String, Object> response = new HashMap<>();
      response.put("roomId", roomId);
      response.put("message", "TTL이 2시간 연장되었습니다");
      response.put("timestamp", java.time.LocalDateTime.now().toString());
      
      log.info("[토론방 TTL 연장] 성공 - roomId: {}", roomId);
      return ResponseEntity.ok(ApiResponse.success(response));
      
    } catch (Exception e) {
      log.error("[토론방 TTL 연장] 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error("TTL 연장에 실패했습니다: " + e.getMessage()));
    }
  }

  @PostMapping("/{roomId}/close")
  @Operation(
    summary = "토론방 종료",
    description = "토론방을 종료하고 정리 작업을 수행합니다."
  )
  public ResponseEntity<ApiResponse<Map<String, Object>>> closeRoom(@PathVariable Long roomId) {
    log.info("[토론방 종료] 요청 수신 - roomId: {}", roomId);
    
    try {
      debateService.closeDebateRoom(roomId);
      
      Map<String, Object> response = new HashMap<>();
      response.put("roomId", roomId);
      response.put("message", "토론방이 종료되었습니다");
      response.put("timestamp", java.time.LocalDateTime.now().toString());
      
      log.info("[토론방 종료] 성공 - roomId: {}", roomId);
      return ResponseEntity.ok(ApiResponse.success(response));
      
    } catch (Exception e) {
      log.error("[토론방 종료] 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
      return ResponseEntity.ok(ApiResponse.error("토론방 종료에 실패했습니다: " + e.getMessage()));
    }
  }

}
