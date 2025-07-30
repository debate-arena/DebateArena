package com.ssafya408.debate.domain.api;

import com.ssafya408.debate.domain.api.service.DebateService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DebateApiController {

  private final DebateService debateService;
//  @MessageMapping("/debate/stt")
//  public void receiveSTTMessage(Principal principal, STTRequest request) {
//    String user = principal.getName();
//    debateService.processSTTMessage(user, request);
//
//  }

}
