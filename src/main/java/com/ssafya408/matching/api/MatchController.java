package com.ssafya408.matching.api;

import com.ssafya408.matching.api.dto.MatchAcceptRequest;
import com.ssafya408.matching.api.dto.MatchApplyRequest;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MatchController {
    private final MatchService matchService;

    //interval 마다 매칭 큐 정보 전송
    @Scheduled(fixedRateString = "${match.status_interval}")
    public void sendMatchingInfo() {
        log.info("main");
        matchService.sendMatchStatus();
    }

    //매시각마다 토픽 정보 받아오고 큐 리셋
    @Scheduled(cron = "0 0 * * * *")
    public void refreshTopicsAndMatchQueue() {
        //토픽정보 갱신
        matchService.refreshTopicsAndMatchQueue();
    }

    @MessageMapping("/match/request")
    public void receiveMatchingRequest(Principal principal, MatchApplyRequest message) {
        matchService.processMatchQueue(principal.getName(), message);
    }

    //참여 응답 확인
    @MessageMapping("/match/acceptance")
    public void receiveMatchAcceptRequest(MatchAcceptRequest message, Principal principal) {
        matchService.receiveMatchAccept(message, principal.getName());
    }

}
