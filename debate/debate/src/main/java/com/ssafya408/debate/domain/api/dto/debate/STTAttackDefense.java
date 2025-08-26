package com.ssafya408.debate.domain.api.dto.debate;

import com.ssafya408.debate.domain.api.dto.stt.STTMessage;
import com.ssafya408.debate.domain.api.dto.stt.STTRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class STTAttackDefense {
  private STTMessage attack;
  private STTMessage defense;

  public STTAttackDefense(String attackUser) {
    attack= STTMessage.initializeSTTMessage(attackUser);
  }

  public void setDefenseUser(String defenseUser) {
    defense= STTMessage.initializeSTTMessage(defenseUser);
  }


  public void addSTTTextAtAttack(STTRequest request) {
    attack.addText(request.getText());
  }

  public void addSTTTextAtDefense(STTRequest request) {
    defense.addText(request.getText());
  }
  public String getAttackTotalMessage() {
    return attack.getJoinedText();
  }

  public String getDefenseTotalMessage() {
    return defense!=null ? defense.getJoinedText():"";
  }
}
