package com.ssafya408.debate.domain.api.dto.stt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class STTRequest {
  private Long roomId;
  private Integer order; //발화 순서

  private String text;
  private Integer idx; // stt 메시지의 순서. 마지막 stt인 경우 0을 보내도록 함
}
