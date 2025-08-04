package com.ssafya408.matching.util;

import com.ssafya408.matching.api.dto.AcceptanceStatusDto;
import com.ssafya408.matching.api.dto.ApiResponse;
import com.ssafya408.matching.api.dto.ChoiceDto;
import com.ssafya408.matching.api.dto.MatchAcceptRequest;
import com.ssafya408.matching.api.dto.MatchApplyRequest;
import com.ssafya408.matching.api.dto.MatchEstablishResponse;
import com.ssafya408.matching.api.dto.WaitingUser;
import com.ssafya408.matching.common.dto.DebateParticipantRequest;
import com.ssafya408.matching.common.topic.service.MatchInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    
    // 스케줄러를 클래스 레벨에서 관리
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    @Value("${match.type}")
    private Integer TYPE ;
    @Value("${match.topic}")
    private Integer TOPIC;
    @Value("${match.choice}")
    private Integer CHOICE;

    private final Integer TIMEWAIT = 30; // 매칭 초대 수락 대기시간
    private Long PENALTY = 1000 * 60 * 10L; // 10분

    List<Long> topicIdxToId;
    Map<Long,Integer> topicIdToIdx;

    private List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue; // 매치 큐
//    private Map<String, Map<String, Boolean>> matchResponses;

    // 매칭 별 수락 대기자 모음
    // 만약 매칭이 취소되면 matchCandidates 에 있는 정보를 불러와서 다시 큐에 넣어준다
//    private Map<String, Map<WaitingUser, MatchApplyRequest>> matchCandidates;
    private Map<String, MatchInfo> matchInfos;
    private Map<String, MatchInfo> activeDebateMatch;


    private Set<String> alreadyMatched;

    public void setMatchUtil(
            List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue,
            Map<String, MatchInfo> matchInfos, Map<String, MatchInfo> activeDebateMatch,
            Set<String> alreadyMatched,
            List<Long> topicIdxToId,
            Map<Long,Integer> topicIdToIdx
    ) {
        this.matchQueue = matchQueue;
        this.matchInfos=matchInfos;
        this.activeDebateMatch=activeDebateMatch;
        this.alreadyMatched = alreadyMatched;
        this.topicIdxToId=topicIdxToId;
        this.topicIdToIdx=topicIdToIdx;
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

    public void processQueue(WaitingUser waitingUser) {
        log.info("[큐 처리 시작] 사용자: {} | 타임스탬프: {}", waitingUser.getUser(), waitingUser.getTimestamp());
        MatchInfo matchInfo = this.getMatchCandidatesFromQueue();

        if (matchInfo!=null) {
            log.info("[매칭 후보 추출] 매칭ID: {} | 총 {}명", matchInfo.getMatchId(), matchInfo.getTotalPlayer());
            // 매칭 참가 수락 여부 처리
            Map<WaitingUser, MatchApplyRequest> userMatchInfo = new HashMap<>();
            for (List<WaitingUser> debateTeam : matchInfo.getTeams()) {
                for (WaitingUser candidate : debateTeam) {
                    // 큐에서 참가자들의 매칭 요청을 poll 해서 매칭 시작 전까지 matchCandidates 에 저장해놓는다
                    // 만약 매칭이 취소되면 matchCandidates 에 있는 정보를 불러와서 다시 큐에 넣어준다
                    MatchApplyRequest matchApplyRequest = this.popUserWaitingInfosAtQueue(candidate.getTimestamp());
                    userMatchInfo.put(candidate, matchApplyRequest);
                }
            }
            matchInfo.saveCandidateInfo(userMatchInfo);
            matchInfos.put(matchInfo.getMatchId(),matchInfo);

            this.startMatching(matchInfo);
        } else {
            log.info("[큐 처리 완료] 매칭 후보 없음 | 사용자: {}", waitingUser.getUser());
        }
    }
    private MatchInfo getMatchCandidatesFromQueue() {
        // 각 큐를 돌면서 매칭 되는 후보를 선택
        List<List<WaitingUser>> teams = new ArrayList<>();
        type:
        for (int type = 0; type < TYPE; type++) { // 타입 (일대일)
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = matchQueue.get(type);
            int player = type + 1;
            int total = player * 2;

            topic:
            for (int topicIdx = 0; topicIdx < TOPIC; topicIdx++) { // 주제
                List<ConcurrentNavigableMap<Long, String>> topicList = typeList.get(topicIdx);

                int anyQueueIdx = CHOICE - 1; // 상관없음을 제외한 큐에서 유저를 poll
                int cnt = 0;
                for (int choice = 0; choice < anyQueueIdx; choice++) {
                    ConcurrentNavigableMap<Long, String> queue = topicList.get(choice);
                    cnt += Math.min(queue.size(), player);
                }
                cnt+=topicList.get(anyQueueIdx).size();
                log.info("[큐 후보군 수]> cnt:{}, total:{}",cnt,total);

                if (cnt >= total) {
                    for (int choice = 0; choice < anyQueueIdx; choice++) {
                        List<WaitingUser> users = new ArrayList<>();
                        ConcurrentNavigableMap<Long, String> queue = topicList.get(choice);
                        int userCnt = 0;

                        while (userCnt < player && !queue.isEmpty()) {
                            Map.Entry<Long, String> e = queue.pollFirstEntry();
                            users.add(WaitingUser.builder()
                                .timestamp(e.getKey())
                                .user(e.getValue())
                                .build());
                            userCnt++;
                        }
                        ConcurrentNavigableMap<Long, String> anyQueue = topicList.get(anyQueueIdx);
                        while (userCnt < player && !anyQueue.isEmpty()) {
                            Map.Entry<Long, String> e = anyQueue.pollFirstEntry();
                            users.add(WaitingUser.builder()
                                .timestamp(e.getKey())
                                .user(e.getValue())
                                .build());
                            userCnt++;
                        }
                        teams.add(users);
                    }
                    Long topicId = convertTopicIdxToId(topicIdx);
                    MatchInfo matchInfo = MatchInfo.generateMatchInfo(topicId, topicIdx, type,teams);

                    return matchInfo;
                }
            }
        }
        return null ;
    }
    public MatchApplyRequest popUserWaitingInfosAtQueue(Long timestamp) {
        List<ChoiceDto> choices = new ArrayList<>();
        for (int i = 0; i < TYPE; i++) { // 타입 별로 큐를 관리.
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = matchQueue.get(i);
            for (int j = 0; j < TOPIC; j++) { // 주제 개수만큼 큐를 가지고 있는다
                List<ConcurrentNavigableMap<Long, String>> topicList = typeList.get(j);
                for (int k = 0; k < CHOICE; k++) {
                    ConcurrentNavigableMap<Long, String> queue = topicList.get(k);
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
    private void startMatching(MatchInfo matchInfo) {
        List<List<WaitingUser>> teams = matchInfo.getTeams();// matchResponses 초기화

        for (int team=0;team<teams.size();team ++) {
            for (WaitingUser user : teams.get(team)) {
                // 공통 포맷으로 매칭 초대 전송

                ApiResponse<MatchEstablishResponse> response = ApiResponse.success(
                    MatchEstablishResponse.builder()
                        .matchId(matchInfo.getMatchId())
                        .email(user.getUser())
                        .team(team)
                        .topicId(matchInfo.getTopicId())
                        .build()
                );

                template.convertAndSendToUser(
                        user.getUser(), "/queue/match/acceptance", response);
                log.info("[수락 요청 전송] 매칭ID: {} | 사용자: {}", matchInfo.getMatchId(), user.getUser());
            }
        }

        log.info("[스케줄러 생성] 매칭ID: {} | {}초 후 응답 평가 예약", matchInfo.getMatchId(), TIMEWAIT);
        
        scheduler.schedule(() -> {
            try {
                log.info("[스케줄러 실행] 매칭ID: {} | 응답 평가 시작", matchInfo.getMatchId());
                evaluateResponse(matchInfo);
                log.info("[스케줄러 완료] 매칭ID: {} | 응답 평가 완료", matchInfo.getMatchId());
            } catch (Exception e) {
                log.error("[스케줄러 오류] 매칭ID: {} | 응답 평가 중 오류: {}", matchInfo.getMatchId(), e.getMessage(), e);
            }
        }, TIMEWAIT, TimeUnit.SECONDS);
    }





    private void evaluateResponse(MatchInfo matchInfo) {

        //기존 매칭 정보 맵에서 제거
        MatchInfo matched = this.matchInfos.remove(matchInfo.getMatchId());
        activeDebateMatch.put(matched.getMatchId(),matched);

        Map<String, Boolean> acceptResponse = matchInfo.getAcceptResponse();
        Map<WaitingUser, MatchApplyRequest> candidates = matchInfo.getCandidates();

        int accept = 0, refuse = 0;
        for (Map.Entry<String, Boolean> e : acceptResponse.entrySet()) {
            if (e.getValue()) // accept==true
                accept++;
            else
                refuse++;
        }

        int totalPlayer = matchInfo.getTotalPlayer();
        log.info("[매칭 응답 평가] 매칭ID: {} | 수락: {}명, 거절: {}명, 총 인원: {}명", 
            matchInfo.getMatchId(), accept, refuse, totalPlayer);

        if (accept == totalPlayer) { // 모두 참여하기를 눌렀을 경우 debate 서버에 전송
            for (Map.Entry<WaitingUser, MatchApplyRequest> e : candidates.entrySet()) {
                alreadyMatched.add(e.getKey().getUser());
            }
            log.info("[매칭 성사] 매칭ID: {} | 참여자: {}", matchInfo.getMatchId(), candidates.keySet());

            // 매칭 성사 알림을 공통 포맷으로 전송
            this.broadcastAcceptanceToDebaters(matchInfo);

            //debate 서버에 요청 전송
            this.requestRoomGenerate(webClient, matchInfo);
        }
        else { // 매칭이 불발 된 경우
            log.info("[매칭 실패] 매칭ID: {} | 일부 거절 또는 미응답", matchInfo.getMatchId());

            // 매칭 실패 알림을 공통 포맷으로 전송
            Map<String, Object> failResult = Map.of(
                    "matchId",  matchInfo.getMatchId(),
                    "message", "매칭이 취소되었습니다. 다시 매칭 대기열에 추가됩니다.");
            ApiResponse<Map<String, Object>> response = ApiResponse.warning(failResult);

            for (Map.Entry<WaitingUser, MatchApplyRequest> e : candidates.entrySet()) {
                WaitingUser userInfo = e.getKey();
                MatchApplyRequest choice = e.getValue();

                // 매칭 실패 알림 전송
                template.convertAndSendToUser(userInfo.getUser(),
                    "/queue/match/acceptance", response);

                // 매칭을 수락하지 않았으면 패널티 부여
                if (!acceptResponse.get(userInfo.getUser())) {
                    userInfo.givePenalty(PENALTY);
                    log.info("[패널티 부여] 사용자: {} | 패널티: {}ms", userInfo.getUser(), PENALTY);
                }

                // 큐에 다시 매칭 정보를 추가
                this.addMatchApplyRequestToQueue(choice, userInfo);


                log.info("[큐 재진입] 사용자: | 큐 상태:");
                printMatchQueueStatus(matchQueue);
            }
        }
    }

    private void broadcastAcceptanceToDebaters(MatchInfo matchInfo) {
        Map<String, Boolean> acceptResponse = matchInfo.getAcceptResponse();
        Map<String, Object> matchResult = Map.of(
                "matchId", matchInfo.getMatchId(),
                "participants", acceptResponse,
                "message", "매칭이 성사되었습니다!");
        ApiResponse<Map<String, Object>> response = ApiResponse.success(matchResult);

        for (String user : acceptResponse.keySet()) {
            template.convertAndSendToUser(user, "/queue/match/acceptance", response);
        }
    }

    private void requestRoomGenerate(WebClient webClient, MatchInfo matchInfo ) {
        log.info("[토론방 생성 요청] 매칭ID: {}, 토픽Idx: {}, 토픽ID: {}, 매칭타입: {}",
            matchInfo.getMatchId(),matchInfo.getTopicIdx(), matchInfo.getTopicId(), matchInfo.getType());
        List<List<WaitingUser>> teams = matchInfo.getTeams();
        DebateParticipantRequest req = DebateParticipantRequest.builder()
            .matchId(matchInfo.getMatchId())
            .topicId(matchInfo.getTopicId())
            .matchType(matchInfo.getType())
            .firstTeam(teams.get(0).stream().map(WaitingUser::getUser).toList())
            .secondTeam(teams.get(1).stream().map(WaitingUser::getUser).toList())
            .build();
        
        // Debate 서버로 데이터 전송
        webClient.post()
            .uri(debateServerUrl + "/rooms")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .retrieve()
            .bodyToMono(com.ssafya408.matching.api.dto.ApiResponse.class)
            .subscribe(
                apiResponse -> {
                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        // data 필드에서 roomId 추출 (LinkedHashMap 형태로 올 수 있음)
                        Object data = apiResponse.getData();
                        Long roomId = null;
                        if (data instanceof java.util.Map) {
                            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                            roomId = ((Number) dataMap.get("roomId")).longValue();
                        }
                        log.info("[토론방 생성 성공] 매칭ID: {} | 방 ID: {}", matchInfo.getMatchId(), roomId);
                    } else {
                        log.error("[토론방 생성 실패] 매칭ID: {} | 응답 상태: {}", matchInfo.getMatchId(), apiResponse.getStatus());
                    }
                },
                error -> {
                    log.error("[토론방 생성 실패] 매칭ID: {} | 오류: {}", matchInfo.getMatchId(), error.getMessage(), error);
                }
            );

    }
    public void printMatchQueueStatus(List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue) {
        for (int type = 0; type < TYPE; type++) {
            for (int topic = 0; topic < TOPIC; topic++) {
                for (int choice = 0; choice < CHOICE; choice++) {
                    ConcurrentNavigableMap<Long, String> queue = matchQueue.get(type).get(topic).get(choice);
                    log.info("[type={}, topic={}, choice={}] 큐 크기: {}", type, topic, choice, queue.size());
                    if (!queue.isEmpty()) {
                        log.info("  유저 목록: {}", queue.values());
                    }



                }
            }
        }
    }

    //수락한 사람들에게 수락 정보를 보낸다.
    public void sendAcceptanceInfoToDebaters(MatchAcceptRequest message, String user) {
        MatchInfo matchInfo = matchInfos.get(message.getMatchId());
        Map<WaitingUser, MatchApplyRequest> candidates =matchInfo.getCandidates();
        for (WaitingUser userInfo : candidates.keySet()) {
            ApiResponse<AcceptanceStatusDto> response = ApiResponse.success(AcceptanceStatusDto.builder()
                    .user(user)
                    .accept(message.getAccept())
                    .build());
            template.convertAndSendToUser(userInfo.getUser(), "/queue/match/acceptance/status", response);

        }

    }

    //큐를 비우기 위해 사람들에게 매칭 취소 정보를 알린다.
    public void sendMatchCancelToDebaters() {
        for (Map.Entry<String,MatchInfo> e: matchInfos.entrySet()) {
            String matchId = e.getKey();
            Map<WaitingUser, MatchApplyRequest> candidates = e.getValue().getCandidates();
            for (WaitingUser userInfo : candidates.keySet()) {
                Map<String,String> res=new HashMap<>();
                res.put("status", "cancel");
                ApiResponse<Map<String, String>> response = ApiResponse.success(res);
                template.convertAndSendToUser(userInfo.getUser(), "/queue/match/cancel", response);
            }
        }

    }

    public void refreshMatchInfo() {
        for (int i = 0; i < TYPE; i++) { // 타입 별로 큐를 관리.
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = matchQueue.get(i);
            for (int j = 0; j < TOPIC; j++) { // 주제 개수만큼 큐를 가지고 있는다
                List<ConcurrentNavigableMap<Long, String>> topicList = typeList.get(j);
                for (int k = 0; k < CHOICE; k++) { // 주제별로 3개의 큐를 가지고 있음
                    ConcurrentNavigableMap<Long, String> dq = topicList.get(k);
                    dq.clear();
                }
            }
        }

        matchInfos.clear();
    }
    public Long convertTopicIdxToId(Integer queueIdx) {
        return topicIdxToId.get(queueIdx);
    }

    public Integer convertTopicIdToIdx(Long originalIdx) {
        return topicIdToIdx.get(originalIdx);
    }


}
