package com.ssafya408.matching.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토론방 생성 요청 DTO")
public class DebateParticipantRequest {
  
  @Schema(description = "매칭 ID", example = "match_12345")
  private String matchId;
  
  @Schema(description = "토론 주제 ID", example = "1")
  private Long topicId;
  
  @Schema(description = "매칭 타입", example = "TWO_ON_TWO")
  private MatchType matchType;
  
  @Schema(description = "첫 번째 진영 토론자 목록", example = "[\"user1\", \"user2\"]")
  private List<String> firstTeam; //첫번째 진영 토론자
  
  @Schema(description = "두 번째 진영 토론자 목록", example = "[\"user3\", \"user4\"]")
  private List<String> secondTeam; // 두번째 진영 토론자
}
