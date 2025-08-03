package com.ssafya408.matching.api.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MatchEstablishResponse {
  private String matchId;
  private String email;
  private Long topicId;
  private Integer team;
}
