package com.ssafya408.matching.domain.api;

import com.ssafya408.matching.domain.api.dto.MatchRequestMessage;
import com.ssafya408.matching.domain.api.dto.MatchStatusMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MatchController {

  private final MatchService matchService;

  private final SimpMessagingTemplate template;

  @MessageMapping("/match/main")
  public MatchStatusMessageResponse sendMatchingInfo() {
    log.info("main");
    //매칭 큐 정보 전송
    MatchStatusMessageResponse matchStatus = matchService.getMatchStatus();
    log.info(""+matchStatus.getMatchStatusDtos().size());
    return  matchStatus;
  }

  @MessageMapping("/match/request")
  public void processMatchingRequest(MatchRequestMessage message) {


  }
}
