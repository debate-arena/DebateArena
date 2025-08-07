package com.ssafya408.debate.domain.api;

import com.ssafya408.debate.domain.api.dto.stt.BattleSTTRequest;
import com.ssafya408.debate.domain.api.dto.stt.OpinionSTTRequest;
import com.ssafya408.debate.domain.api.service.DebateService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DebateApiController {

  private final DebateService debateService;
  @MessageMapping("/debate/{roomId}/stt/opinion")
  public void receiveOpinionSTTMessage(Principal principal,@DestinationVariable Long roomId, OpinionSTTRequest request) {
    String user = principal.getName();
    log.info("의견 STT 메시지 수신 - 사용자: {}, 방ID: {}, 텍스트: {}",
        user, roomId,  request.getText());
    
    debateService.broadcastSTTMessage(user,roomId,request);

    debateService.processOpinionSTTMessage(user,roomId, request);

  }

  @MessageMapping("/debate/{roomId}/stt/battle")
  public void receiveBattleSTTMessage(Principal principal, @DestinationVariable Long roomId,
      BattleSTTRequest request) {
    String user = principal.getName();
    log.info("배틀 STT 메시지 수신 - 사용자: {}, 방ID: {}, 텍스트: {}",
        user, roomId, request.getText());

    debateService.broadcastSTTMessage(user, roomId, request);
    debateService.processBattleSTTMessage(user, roomId, request);

  }

  @MessageMapping("/debate/join")
  public void joinMatch(Principal principal, Long roomId) {
    String user = principal.getName();
    log.info("방 입장 - 사용자: {}, 방ID: {}", user, roomId);

    debateService.userJoinMatch(user, roomId);

  }

}
