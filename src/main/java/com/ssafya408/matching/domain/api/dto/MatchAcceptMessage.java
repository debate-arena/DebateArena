package com.ssafya408.matching.domain.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchAcceptMessage {
  private String matchId;
  private Boolean accept;
}
