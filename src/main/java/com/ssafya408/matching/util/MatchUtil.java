package com.ssafya408.matching.util;

import com.ssafya408.matching.api.dto.AcceptanceStatusDto;
import com.ssafya408.matching.api.dto.ApiResponse;
import com.ssafya408.matching.api.dto.ChoiceDto;
import com.ssafya408.matching.api.dto.MatchAcceptRequest;
import com.ssafya408.matching.api.dto.MatchApplyRequest;
import com.ssafya408.matching.api.dto.WaitingUser;
import com.ssafya408.matching.common.dto.DebateParticipantRequest;
import com.ssafya408.matching.common.dto.MatchType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class MatchUtil {
    private final WebClient webClient;
    @Value("${debate.server.url}")
    private String debateServerUrl;
    private static final Logger log = LoggerFactory.getLogger(MatchUtil.class);
    private final SimpMessagingTemplate template;
    private final Integer TYPE = 2, TITLE = 5, CHOICE = 3;
    private final Integer TIMEWAIT = 30; // 매칭 초대 수락 대기시간
    private Long PENALTY = 1000 * 60 * 10L; // 10분

    private List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue; // 매치 큐
    private Map<String, Map<String, Boolean>> matchResponses;

    // 매칭 별 수락 대기자 모음
    // 만약 매칭이 취소되면 matchCandidates 에 있는 정보를 불러와서 다시 큐에 넣어준다
    private Map<String, Map<WaitingUser, MatchApplyRequest>> matchCandidates;

    private Set<String> alreadyMatched;

    public void setMatchUtil(
            List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue,
            Map<String, Map<WaitingUser, MatchApplyRequest>> matchCandidates,
            Map<String, Map<String, Boolean>> matchResponses, Set<String> alreadyMatched) {
        this.matchQueue = matchQueue;
        this.matchCandidates = matchCandidates;
        this.matchResponses = matchResponses;
        this.alreadyMatched = alreadyMatched;
    }

    public void addMatchApplyRequestToQueue(MatchApplyRequest req,
                                            WaitingUser waitingUser) {

        List<ChoiceDto> choiceDtos = req.getChoices();
        // 1. 사용자가 선택한 데이터를 기반으로 큐에 추가
        for (ChoiceDto choiceDto : choiceDtos) {
            Integer matchType = choiceDto.getMatchType();
            Integer matchTitle = choiceDto.getMatchTitle();
            Integer choice = choiceDto.getChoice();

            // 1. 사용자 선택을 기반으로 해당하는 큐에 추가
            matchQueue.get(matchType).get(matchTitle).get(choice)
                    .put(waitingUser.getTimestamp(), waitingUser.getUser());

        }
    }

    public void processQueue(String matchId, WaitingUser waitingUser) {
        List<List<WaitingUser>> candidates = selectMatchCandidates();

        if (!candidates.isEmpty()) {
            log.info("[매칭 후보 추출] 매칭ID: {} | 후보자: {}", matchId, candidates);
            // 매칭 참가 수락 여부 처리
            Map<WaitingUser, MatchApplyRequest> userMatchInfo = new HashMap<>();
            for (List<WaitingUser> debateTeam : candidates) {
                for (WaitingUser candidate : debateTeam) {
                    // 큐에서 참가자들의 매칭 요청을 poll 해서 매칭 시작 전까지 matchCandidates 에 저장해놓는다
                    // 만약 매칭이 취소되면 matchCandidates 에 있는 정보를 불러와서 다시 큐에 넣어준다
                    MatchApplyRequest matchApplyRequest = popUserWaitingInfosAtQueue(candidate.getTimestamp());
                    userMatchInfo.put(candidate, matchApplyRequest);
                }
            }
            matchCandidates.put(matchId, userMatchInfo);

            log.info("[매칭 성사 대기] 매칭ID: {} | {}명에게 수락 요청 전송", matchId, userMatchInfo.size());
            startMatching(matchId, candidates);
        }
    }

    private void startMatching(String matchId, List<List<WaitingUser>> candidates) {
        // matchResponses 초기화
        matchResponses.put(matchId, new ConcurrentHashMap<>());
        int total = candidates.size() * candidates.getFirst().size();
        for (List<WaitingUser> team : candidates) {
            for (WaitingUser user : team) {
                // 공통 포맷으로 매칭 초대 전송
                Map<String, String> matchData = Map.of("matchId", matchId);
                ApiResponse<Map<String, String>> response = ApiResponse.info(matchData);

                template.convertAndSendToUser(
                        user.getUser(), "/queue/match/acceptance", response);
                log.info("[수락 요청 전송] 매칭ID: {} | 사용자: {}", matchId, user.getUser());
            }
        }

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            evaluateResponse(matchId, candidates, total);
        }, TIMEWAIT, TimeUnit.SECONDS);
    }

    public MatchApplyRequest popUserWaitingInfosAtQueue(Long timestamp) {
        List<ChoiceDto> choices = new ArrayList<>();
        for (int i = 0; i < TYPE; i++) { // 타입 별로 큐를 관리.
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = matchQueue.get(i);
            for (int j = 0; j < TITLE; j++) { // 주제 개수만큼 큐를 가지고 있는다
                List<ConcurrentNavigableMap<Long, String>> titleList = typeList.get(j);
                for (int k = 0; k < CHOICE; k++) {
                    ConcurrentNavigableMap<Long, String> queue = titleList.get(k);
                    String user = queue.remove(timestamp);
                    if (user != null) { // 큐에 유저가 존재한다면
                        ChoiceDto choice = ChoiceDto.builder()
                                .matchType(i)
                                .matchTitle(j)
                                .choice(k)
                                .build();
                        choices.add(choice);
                        break;
                    }
                }
            }
        }
        return MatchApplyRequest.builder()
                .choices(choices)
                .build();
    }

    public List<List<WaitingUser>> selectMatchCandidates() {
        // 각 큐를 돌면서 매칭 되는 후보를 선택
        List<List<WaitingUser>> candidates = new ArrayList<>();
        type:
        for (int type = 0; type < TYPE; type++) { // 타입 (일대일)
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = matchQueue.get(type);
            int player = type + 1;
            int total = player * 2;
            title:
            for (int title = 0; title < TITLE; title++) { // 주제
                List<ConcurrentNavigableMap<Long, String>> titleList = typeList.get(title);

                int cnt = 0;
                for (int choice = 0; choice < CHOICE; choice++) {
                    ConcurrentNavigableMap<Long, String> queue = titleList.get(choice);
                    cnt += Math.min(queue.size(), player);
                }

                if (cnt >= total) { // remain 이 0이하면 큐를 잡을 수 있는 상태
                    int anyQueueIdx = CHOICE - 1; // 상관없음을 제외한 큐에서 유저를 poll
                    for (int choice = 0; choice < anyQueueIdx; choice++) {
                        List<WaitingUser> users = new ArrayList<>();
                        ConcurrentNavigableMap<Long, String> queue = titleList.get(choice);
                        int userCnt = 0;

                        while (userCnt < player && !queue.isEmpty()) {
                            Map.Entry<Long, String> e = queue.pollFirstEntry();
                            users.add(WaitingUser.builder()
                                    .timestamp(e.getKey())
                                    .user(e.getValue())
                                    .build());
                            userCnt++;
                        }
                        ConcurrentNavigableMap<Long, String> anyQueue = titleList.get(anyQueueIdx);
                        while (userCnt < player && !anyQueue.isEmpty()) {
                            Map.Entry<Long, String> e = anyQueue.pollFirstEntry();
                            users.add(WaitingUser.builder()
                                    .timestamp(e.getKey())
                                    .user(e.getValue())
                                    .build());
                            userCnt++;
                        }
                        candidates.add(users);
                    }
                    break type;
                }
            }
        }
        return candidates;
    }

    private void evaluateResponse(String matchId, List<List<WaitingUser>> userList, int total) {
        Map<String, Boolean> userResponses = matchResponses.remove(matchId);
        Map<WaitingUser, MatchApplyRequest> candidates = matchCandidates.remove(matchId);

        int accept = 0, refuse = 0;
        for (Map.Entry<String, Boolean> e : userResponses.entrySet()) {
            if (e.getValue()) // accept==true
                accept++;
            else
                refuse++;
        }
        log.info("[수락 응답 집계] 매칭ID: {} | 수락: {} | 거절: {} | 전체: {}", matchId, accept, refuse, total);

        if (accept == total) { // 모두 참여하기를 눌렀을 경우 debate 서버에 전송
            for (Map.Entry<WaitingUser, MatchApplyRequest> e : candidates.entrySet()) {
                alreadyMatched.add(e.getKey().getUser());
            }
            log.info("[매칭 성사] 매칭ID: {} | 참여자: {}", matchId, candidates.keySet());

            // 매칭 성사 알림을 공통 포맷으로 전송
            broadcastAcceptanceToDebaters(matchId, candidates);

            Long topicId = 1L; // 수정 필요
            // 매칭 타입 결정 (userList의 크기로 판단)
            MatchType matchType = userList.size() == 1 ? MatchType.ONE_ON_ONE : MatchType.TWO_ON_TWO;
            
            requestRoomGenerate(webClient, matchId, topicId, matchType,
                userList.getFirst().stream().map(WaitingUser::getUser).toList(),
                userList.getLast().stream().map(WaitingUser::getUser).toList());
        }
        else { // 매칭이 불발 된 경우
            log.info("[매칭 실패] 매칭ID: {} | 일부 거절 또는 미응답", matchId);

            // 매칭 실패 알림을 공통 포맷으로 전송
            Map<String, Object> failResult = Map.of(
                    "matchId", matchId,
                    "message", "매칭이 취소되었습니다. 다시 매칭 대기열에 추가됩니다.");
            ApiResponse<Map<String, Object>> response = ApiResponse.warning(failResult);

            for (Map.Entry<WaitingUser, MatchApplyRequest> e : candidates.entrySet()) {
                WaitingUser userInfo = e.getKey();
                MatchApplyRequest choice = e.getValue();

                // 매칭 실패 알림 전송
                template.convertAndSendToUser(userInfo.getUser(), "/queue/match/acceptance", response);

                // 매칭을 수락하지 않았으면 패널티 부여
                if (!userResponses.get(userInfo.getUser())) {
                    userInfo.givePenalty(PENALTY);
                    log.info("[패널티 부여] 사용자: {} | 패널티: {}ms", userInfo.getUser(), PENALTY);
                }

                // 큐에 다시 매칭 정보를 추가
                addMatchApplyRequestToQueue(choice, userInfo);
                log.info("[큐 재진입] 사용자: {} | 큐 상태:");
                printMatchQueueStatus(matchQueue);
            }
        }
    }

    private void broadcastAcceptanceToDebaters(String matchId, Map<WaitingUser, MatchApplyRequest> candidates) {
        Map<String, Object> matchResult = Map.of(
                "matchId", matchId,
                "participants", candidates.keySet(),
                "message", "매칭이 성사되었습니다!");
        ApiResponse<Map<String, Object>> response = ApiResponse.success(matchResult);

        for (Map.Entry<WaitingUser, MatchApplyRequest> e : candidates.entrySet()) {
            template.convertAndSendToUser(e.getKey().getUser(), "/queue/match/acceptance", response);
        }
    }

    private void requestRoomGenerate(WebClient webClient, String matchId, Long topicId,
            MatchType matchType, List<String> firstTeam, List<String> secondTeam
        ) {
        log.info("[토론방 생성 요청] 매칭ID: {}, 토픽ID: {}, 매칭타입: {}", matchId, topicId, matchType);
        log.info("[토론방 생성 요청] 첫 번째 팀: {}, 두 번째 팀: {}", firstTeam, secondTeam);
        
        DebateParticipantRequest req = DebateParticipantRequest.builder()
            .matchId(matchId)
            .topicId(topicId)
            .matchType(matchType)
            .firstTeam(firstTeam)
            .secondTeam(secondTeam)
            .build();
        
        // Debate 서버로 데이터 전송
        webClient.post()
            .uri(debateServerUrl + "/rooms")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .retrieve()
            .bodyToMono(DebateParticipantRequest.class) // 응답을 String으로 받아서 로깅
            .subscribe(
                result -> {
                    log.info("[토론방 생성 성공] 매칭ID: {} | 응답: {}", matchId, result.getMatchId());
                },
                error -> {
                    log.error("[토론방 생성 실패] 매칭ID: {} | 오류: {}", matchId, error.getMessage(), error);
                }
            );
        
    }


    private void removeParticipant(List<WaitingUser> users) {
        for (List<List<ConcurrentNavigableMap<Long, String>>> typeList : matchQueue) { // 타입 (일대일)
            for (List<ConcurrentNavigableMap<Long, String>> titleList : typeList) { // 주제
                for (ConcurrentNavigableMap<Long, String> queue : titleList) { // 주제에 찬,반, 상관 없음 큐 순회
                    for (WaitingUser user : users)
                        queue.remove(user.getTimestamp());
                }
            }
        }
    }

    public void printMatchQueueStatus(List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue) {
        for (int type = 0; type < TYPE; type++) {
            for (int title = 0; title < TITLE; title++) {
                for (int choice = 0; choice < CHOICE; choice++) {
                    ConcurrentNavigableMap<Long, String> queue = matchQueue.get(type).get(title).get(choice);
                    log.info("[type={}, title={}, choice={}] 큐 크기: {}", type, title, choice, queue.size());
                    if (!queue.isEmpty()) {
                        log.info("  유저 목록: {}", queue.values());
                    }
                }
            }
        }
    }


    public void sendAcceptantInfo(MatchAcceptRequest message, String user) {
        Map<WaitingUser, MatchApplyRequest> candidates = matchCandidates.get(message.getMatchId());
        for (WaitingUser userInfo : candidates.keySet()) {
            ApiResponse<AcceptanceStatusDto> response = ApiResponse.success(AcceptanceStatusDto.builder()
                    .user(user)
                    .accept(message.getAccept())
                    .build());
            template.convertAndSendToUser(userInfo.getUser(), "/queue/match/acceptance/status", response);

        }

    }
}
