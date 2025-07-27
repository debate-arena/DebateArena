package com.ssafya408.matching.domain.api;

import com.ssafya408.matching.domain.api.dto.MatchAcceptMessage;
import com.ssafya408.matching.domain.api.dto.MatchApplyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MatchController {
    private final MatchService matchService;

    //매칭 큐 정보 전송
    @Scheduled(fixedRateString = "${match.status_interval}")
    public void sendMatchingInfo() {
        log.info("main");
        matchService.sendMatchStatus();
    }

    @MessageMapping("/match/request")
    public void receiveMatchingRequest(Principal principal, MatchApplyRequest message) {
        matchService.processMatchQueue(principal.getName(), message);
    }

    //참여 응답 확인
    @MessageMapping("/match/acceptance")
    public void receiveMatchAcceptMessage(MatchAcceptMessage message, Principal principal) {
        matchService.receiveMatchAccept(message, principal.getName());
    }

}
