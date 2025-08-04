package com.ssafya408.debate.domain.api.dto.stt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BattleSTTRequest extends STTRequest {
  private Boolean isSiege; //공격 턴인지 판별, true:공격, false: 방어, null: 해당 없음(1단계)
}
