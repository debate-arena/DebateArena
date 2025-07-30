package com.ssafya408.matching.api;

import com.ssafya408.matching.api.dto.ApiResponse;
import com.ssafya408.matching.api.dto.MatchAcceptRequest;
import com.ssafya408.matching.api.dto.MatchApplyRequest;
import com.ssafya408.matching.api.dto.MatchStatusDto;
import com.ssafya408.matching.api.dto.WaitingUser;
import com.ssafya408.matching.util.MatchUtil;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {
    // 매칭 타입 개수 eg) 1:1 => [0], 2:2 => [1], ...
    // 주제 타입 개수 eg) 주제1, 주제2, 주제3...
    // 선택지 개수 eg) 1번 선택, 2번 선택, 3번 선택...
    private final Integer TYPE = 2, TITLE = 5, CHOICE = 3;
    private final Integer ACCEPT = 0, REFUSE = 1;
    private final MatchUtil matchUtil;
    private final SimpMessagingTemplate template;
    private List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue; // 매치 큐
    private Map<String, Map<String, Boolean>> matchResponses;

    // 매칭 별 수락 대기자 모음
    // 만약 매칭이 취소되면 matchCandidates 에 있는 정보를 불러와서 다시 큐에 넣어준다
    private Map<String, Map<WaitingUser, MatchApplyRequest>> matchCandidates;

    private Set<String> alreadyMatched;

    @PostConstruct
    public void initMatchService() {
        matchQueue = new ArrayList<>();
        for (int i = 0; i < TYPE; i++) { // 타입 별로 큐를 관리.
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = new ArrayList<>();
            for (int j = 0; j < TITLE; j++) { // 주제 개수만큼 큐를 가지고 있는다
                List<ConcurrentNavigableMap<Long, String>> titleList = new ArrayList<>();
                for (int k = 0; k < CHOICE; k++) { // 주제별로 3개의 큐를 가지고 있음

                    ConcurrentNavigableMap<Long, String> dq = new ConcurrentSkipListMap<>();
                    titleList.add(dq);
                }
                typeList.add(titleList);
            }
            matchQueue.add(typeList);
        }
        matchResponses = new ConcurrentHashMap<>();
        matchCandidates = new HashMap<>();
        alreadyMatched = new HashSet<>();
        matchUtil.setMatchUtil(matchQueue, matchCandidates, matchResponses, alreadyMatched);
    }

    public void sendMatchStatus() {
        List<MatchStatusDto> matchStatusDtos = new ArrayList<>();
        for (int i = 0; i < TYPE; i++) { // 타입 별로 큐를 관리.
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = matchQueue.get(i);
            for (int j = 0; j < TITLE; j++) { // 주제 개수만큼 큐를 가지고 있는다
                List<ConcurrentNavigableMap<Long, String>> titleList = typeList.get(j);
                MatchStatusDto status = MatchStatusDto.builder()
                        .matchType(i)
                        .matchTitle(j)
                        .firstQueueSize(titleList.get(0).size())
                        .secondQueueSize(titleList.get(1).size())
                        .thirdQueueSize(titleList.get(2).size())
                        .build();
                matchStatusDtos.add(status);
            }
        }

        // 공통 포맷으로 전송
        ApiResponse<List<MatchStatusDto>> response = ApiResponse.success(matchStatusDtos);
        template.convertAndSend("/sub/match/status", response);
    }

    public void processMatchQueue(String user, MatchApplyRequest req) {
        if (alreadyMatched.contains(user))
            return;

        // {user 정보, 큐 진입시점} 쌍
        WaitingUser matchUserInfo = WaitingUser.builder()
                .user(user)
                .timestamp(System.nanoTime())
                .build();

        log.info("[매칭 요청] 사용자: {} | 요청: {}", user, req);

        // 1. 사용자가 선택한 데이터를 기반으로 큐에 추가
        matchUtil.addMatchApplyRequestToQueue(req, matchUserInfo);
        log.info("[큐 진입] 사용자: {} | 큐 상태:", user);
        matchUtil.printMatchQueueStatus(matchQueue);

        String matchId = UUID.randomUUID().toString();

        // 2. 현재 큐를 판단해서 매칭이 가능하다면, [1번,2번] 진영의 토론 후보들을 추출한다(상관없음 포함);
        matchUtil.processQueue(matchId, matchUserInfo);

        matchUtil.printMatchQueueStatus(matchQueue);
    }


    // 참가자들에게 보낸 참가 확인에 대한 답을 matchId에 기록
    // 30초 후에 기록한 내용을 바탕으로 방 생성 or 매칭 취소 결정
    public void receiveMatchAccept(MatchAcceptRequest message, String user) {
        String matchId = message.getMatchId();
        Boolean answer = message.getAccept();
        log.info("[수락 응답] 사용자: {} | 매칭ID: {} | 응답: {}", user, matchId, answer);
        matchResponses.get(matchId).put(user, answer);

        // 매칭 참여자들에게 실시간 수락 정보를 보낸다
        matchUtil.sendAcceptantInfo(message, user);
    }

}
