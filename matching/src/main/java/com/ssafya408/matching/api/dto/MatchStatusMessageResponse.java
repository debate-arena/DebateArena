package com.ssafya408.matching.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchStatusMessageResponse {
  private List<MatchStatusDto> matchStatusDtos;
}
