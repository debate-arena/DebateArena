package com.ssafya408.debate.domain.api.dto.stt;

import com.ssafya408.debate.domain.api.dto.debate.DebateTurn;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
public class STTMessage {
  private String user;
  private List<String> texts;

  private STTMessage(String user) {
    this.user=user;
    texts = new ArrayList<>();
  }

  public static STTMessage initializeSTTMessage(String user) {
    return new STTMessage(user);
  }

  public String getJoinedText() {
    return String.join(" ",texts);
  }

  public void addText(String text){
    texts.add(text);
  }

}
