package com.ssafya408.matching.domain.api;

import com.ssafya408.matching.domain.api.dto.MatchAcceptMessage;
import com.ssafya408.matching.domain.api.dto.MatchRequestMessage;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MatchController {
  private final MatchService matchService;

  //
  @Scheduled(fixedRateString = "${match.status_interval}")
  public void sendMatchingInfo() {
    log.info("main");
    //매칭 큐 정보 전송
     matchService.sendMatchStatus();
  }

  @MessageMapping("/match/request")
  public void receiveMatchingRequest(Principal principal, MatchRequestMessage message) {
    matchService.processMatchQueue(principal.getName(),message);
  }

  @MessageMapping("/match/acceptance")
  public void receiveMatchAcceptMessage(MatchAcceptMessage message, Principal principal)  {
    matchService.receiveMatchAccept(message, principal.getName());
  }
}
