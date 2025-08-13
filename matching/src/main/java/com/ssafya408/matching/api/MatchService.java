package com.ssafya408.matching.api;

import com.ssafya408.matching.api.dto.MatchAcceptRequest;
import com.ssafya408.matching.api.dto.MatchApplyRequest;
import com.ssafya408.matching.api.dto.MatchStatusDto;
import com.ssafya408.matching.api.dto.WaitingUser;
import com.ssafya408.matching.common.dto.ApiResponse;
import com.ssafya408.matching.common.topic.dto.TopicDto;
import com.ssafya408.matching.common.topic.service.MatchInfo;
import com.ssafya408.matching.common.topic.service.TopicService;
import com.ssafya408.matching.util.MatchUtil;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Getter
@Slf4j
public class MatchService {
    private final TopicService topicService;

    // 매칭 타입 개수 eg) 1:1 => [0], 2:2 => [1], ...
    // 주제 타입 개수 eg) 주제1, 주제2, 주제3...
    // 선택지 개수 eg) 1번 선택, 2번 선택, 3번 선택...
    @Value("${match.type}")
    private Integer TYPE ;
    @Value("${match.topic}")
    private Integer TOPIC;
    @Value("${match.choice}")
    private Integer CHOICE;
    private final Integer ACCEPT = 0, REFUSE = 1;
    private final MatchUtil matchUtil;
    private final SimpMessagingTemplate template;
    private List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue; // 매치 큐
//    private Map<String, Map<String, Boolean>> matchResponses;

    // 매칭 별 수락 대기자 모음
    // 만약 매칭이 취소되면 matchCandidates 에 있는 정보를 불러와서 다시 큐에 넣어준다
//    private Map<String, Map<WaitingUser, MatchApplyRequest>> matchCandidates;
    private Map<String, MatchInfo> matchInfos;

    private  Map<String, MatchInfo> activeDebateMatch;

    private List<Long> topicIdxToId; //큐 idx를 실제 topic id로 변환
    private Map<Long,Integer> topicIdToIdx; //실제 topic_id -> 큐 topic idx 로 변환
    private Set<String> alreadyMatched;

    @PostConstruct
    public void initMatchService() {
        matchQueue = new ArrayList<>();
        //매칭 큐 및 관련 정보 초기화
        for (int i = 0; i < TYPE; i++) { // 타입 별로 큐를 관리.
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = new ArrayList<>();
            for (int j = 0; j < TOPIC; j++) { // 주제 개수만큼 큐를 가지고 있는다
                List<ConcurrentNavigableMap<Long, String>> topicList = new ArrayList<>();
                for (int k = 0; k < CHOICE; k++) { // 주제별로 3개의 큐를 가지고 있음
                    ConcurrentNavigableMap<Long, String> dq = new ConcurrentSkipListMap<>();
                    topicList.add(dq);
                }
                typeList.add(topicList);
            }
            matchQueue.add(typeList);
        }
        matchInfos = new ConcurrentHashMap<>();
        activeDebateMatch=new ConcurrentHashMap<>();

        //topic 관련 인덱스 자료 구조
        topicIdToIdx = new HashMap<>();
        topicIdxToId = new ArrayList<>();

        //현재 토픽 정보 가져오기
        getCurrentTopicsFromRedis();

        alreadyMatched = new HashSet<>();
        matchUtil.setMatchUtil(matchQueue, matchInfos, activeDebateMatch,
            alreadyMatched,topicIdxToId, topicIdToIdx);
    }

    public void processMatchQueue(String user, MatchApplyRequest req) {
//        if (alreadyMatched.contains(user))
//            return;

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
        // 2. 현재 큐를 판단해서 매칭이 가능하다면, [1번,2번] 진영의 토론 후보들을 추출한다(상관없음 포함);
        matchUtil.processQueue( matchUserInfo);

        matchUtil.printMatchQueueStatus(matchQueue);
    }

    private void getCurrentTopicsFromRedis() {
        //데이터 초기화
        topicIdxToId.clear();
        topicIdToIdx.clear();
        
        List<TopicDto> currentTopics = topicService.getTopics().getCurrentTopics();

        for (int i = 0; i < TOPIC; i++) {
            topicIdToIdx.put(currentTopics.get(i).getId(), i);
            topicIdxToId.add(currentTopics.get(i).getId());
        }
    }

    public void refreshTopicsAndMatchQueue() {
        // 메인에서 갱신한 토픽 정보들을 가져온다
        getCurrentTopicsFromRedis();
        // 지금 큐에 있는 사람들에게 큐가 취소 된다는 것을 알리기
        matchUtil.sendMatchCancelToDebaters();

        // 큐 정보 리셋
        matchUtil.refreshMatchInfo();
        
        //redis에서 토픽정보 가져옴
        getCurrentTopicsFromRedis();
    }

    public void sendMatchStatus() {
        List<MatchStatusDto> matchStatusDtos = new ArrayList<>();
        for (int i = 0; i < TYPE; i++) { // 타입 별로 큐를 관리.
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = matchQueue.get(i);
            for (int j = 0; j < TOPIC; j++) { // 주제 개수만큼 큐를 가지고 있는다
                List<ConcurrentNavigableMap<Long, String>> topicList = typeList.get(j);
                MatchStatusDto status = MatchStatusDto.builder()
                        .matchType(i)
                        .matchTitle(j)
                        .firstQueueSize(topicList.get(0).size())
                        .secondQueueSize(topicList.get(1).size())
                        .thirdQueueSize(topicList.get(2).size())
                        .build();
                matchStatusDtos.add(status);
            }
        }

        // 공통 포맷으로 전송
        ApiResponse<List<MatchStatusDto>> response = ApiResponse.success(matchStatusDtos);
        template.convertAndSend("/sub/match/status", response);
    }


    // 참가자들에게 보낸 참가 확인에 대한 답을 matchId에 기록
    // 30초 후에 기록한 내용을 바탕으로 방 생성 or 매칭 취소 결정
    public void receiveMatchAccept(MatchAcceptRequest message, String user) {
        String matchId = message.getMatchId();
        Boolean answer = message.getAccept();
        log.info("[수락 응답] 사용자: {} | 매칭ID: {} | 응답: {}", user, matchId, answer);

        MatchInfo matchInfo = matchInfos.get(matchId);
        matchInfo.updateMatchResponse(user,answer);

        // 매칭 참여자들에게 실시간 수락 정보를 보낸다
        matchUtil.sendAcceptanceInfoToDebaters(message);
    }

    /**
     * alreadyMatched 세트를 초기화합니다 (테스트용)
     */
    public void clearAlreadyMatched() {
        alreadyMatched.clear();
        log.info("[테스트] alreadyMatched 초기화 완료 - 모든 사용자가 다시 매칭 가능");
    }

    /**
     * 현재 alreadyMatched에 포함된 사용자 수를 반환합니다 (테스트용)
     */
    public int getAlreadyMatchedCount() {
        return alreadyMatched.size();
    }

    /**
     * 현재 alreadyMatched에 포함된 사용자 목록을 반환합니다 (테스트용)
     */
    public Set<String> getAlreadyMatchedUsers() {
        return new HashSet<>(alreadyMatched);
    }

}
