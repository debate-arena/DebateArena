package com.ssafya408.debate.domain.api.dto.stt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class  STTRequest {
  private Long roomId;

  private String text;
}
