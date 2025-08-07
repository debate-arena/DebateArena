package com.ssafya408.debate.domain.api.dto.stt.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BroadcastResponse {
  private String user;
  private String text;
}
