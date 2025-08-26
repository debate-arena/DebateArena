package com.ssafya408.debatearena.api;

import com.ssafya408.debatearena.api.dto.ActiveRoomsListResponseDto;
import com.ssafya408.debatearena.service.DebateRoomService;
import com.ssafya408.debatearena.service.topic.dto.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
@Tag(name = "토론방 API", description = "토론방 정보 조회 관련 API")
@Slf4j
public class DebateRoomController {

    private final DebateRoomService debateRoomService;

    @GetMapping("/active")
    @Operation(
        summary = "활성 토론방 목록 조회",
        description = "Redis에서 현재 활성화된 모든 토론방의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "활성 토론방 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ActiveRoomsListResponseDto.class),
                examples = @ExampleObject(
                    name = "성공 예시",
                    value = """
                        {
                          "status": "success",
                          "data": {
                            "activeRooms": [
                              {
                                "roomId": 123,
                                "type": 0,
                                "topicId": 1,
                                "topicText": "인공지능의 윤리적 문제",
                                "firstOption": "찬성",
                                "secondOption": "반대",
                                "status": "IN_PROGRESS",
                                "webRTCStatus": "CONNECTED",
                                "firstTeam": [
                                  {"order": 0, "speaker": "user1"},
                                  {"order": 1, "speaker": "user2"}
                                ],
                                "secondTeam": [
                                  {"order": 0, "speaker": "user3"},
                                  {"order": 1, "speaker": "user4"}
                                ],
                                "createdAt": "2024-01-01T12:00:00",
                                "participantCount": 4
                              }
                            ],
                            "totalCount": 1,
                            "timestamp": "2024-01-01T12:00:00",
                            "message": "활성 토론방 목록을 성공적으로 조회했습니다."
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<ActiveRoomsListResponseDto>> getActiveRoomsWithDetails() {
        log.info("[활성 토론방 상세 목록 조회] 요청 수신");
        
        try {
            ActiveRoomsListResponseDto response = debateRoomService.getActiveRoomsWithDetails();
            
            log.info("[활성 토론방 상세 목록 조회] 성공 - 활성 방 개수: {}", response.getTotalCount());
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("[활성 토론방 상세 목록 조회] 실패 - error: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("활성 토론방 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }
}
