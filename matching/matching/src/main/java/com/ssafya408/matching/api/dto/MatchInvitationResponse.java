package com.ssafya408.matching.api.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MatchInvitationResponse {
  private String matchId;
  private Long topicId;
  private Integer team;
  private Integer type;
}
