package com.ssafya408.debate.domain.api.dto.debate;

import com.ssafya408.debate.domain.api.dto.stt.STTMessage;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class STTAttackDefense {
  private STTMessage attack;
  private STTMessage defense;
}
