package com.ssafya408.matching.domain.api;

import com.ssafya408.matching.domain.api.dto.ChoiceDto;
import com.ssafya408.matching.domain.api.dto.MatchAcceptMessage;
import com.ssafya408.matching.domain.api.dto.MatchStatusDto;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class MatchService {
    //매칭 타입 개수 eg) 1:1 => [0], 2:2 => [1], ...
    //주제 타입 개수 eg) 주제1, 주제2, 주제3...
    //선택지 개수 eg) 1번 선택, 2번 선택, 3번 선택...
    private final Integer TYPE = 2, TITLE = 5, CHOICE = 3;
    private final Integer TIMEWAIT = 30; //매칭 초대 수락 대기시간
    private final Integer ACCEPT = 0, REFUSE = 1;
    private final SimpMessagingTemplate template;
    private List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue; //매치 큐
    private Map<String, Map<String, Boolean>> matchResponses;
    private Set<String> alreadyMatched, canceled;

    @PostConstruct
    public void initMatchService() {
        matchQueue = new ArrayList<>();
        for (int i = 0; i < TYPE; i++) { //타입 별로 큐를 관리.
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = new ArrayList<>();
            for (int j = 0; j < TITLE; j++) { //주제 개수만큼 큐를 가지고 있는다
                List<ConcurrentNavigableMap<Long, String>> titleList = new ArrayList<>();
                for (int k = 0; k < CHOICE; k++) { //주제별로 3개의 큐를 가지고 있음

                    ConcurrentNavigableMap<Long, String> dq = new ConcurrentSkipListMap<>();
                    titleList.add(dq);
                }
                typeList.add(titleList);
            }
            matchQueue.add(typeList);
        }
        matchResponses = new ConcurrentHashMap<>();

        alreadyMatched = new HashSet<>();
        canceled = new HashSet<>();
    }

    public void sendMatchStatus() {
        List<MatchStatusDto> matchStatusDtos = new ArrayList<>();
        for (int i = 0; i < TYPE; i++) { //타입 별로 큐를 관리.
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = matchQueue.get(i);
            for (int j = 0; j < TITLE; j++) { //주제 개수만큼 큐를 가지고 있는다
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

        template.convertAndSend("/sub/match/status", matchStatusDtos);
    }

    public void processMatchQueue(String user, List<ChoiceDto> choiceDtos) {
        // 1. 사용자가 선택한 데이터를 기반으로 큐에 추가
        for (ChoiceDto choiceDto : choiceDtos) {
            Integer matchType = choiceDto.getMatchType();
            Integer matchTitle = choiceDto.getMatchTitle();
            Integer choice = choiceDto.getChoice();

            //1. 사용자 선택을 기반으로 해당하는 큐에 추가
            matchQueue.get(matchType).get(matchTitle).get(choice)
                    .put(System.nanoTime(), user);

            //2. 현재 큐를 판단해서 매칭이 가능하다면
            List<WaitingUser> users = selectMatchUsers(matchType, matchTitle);
            if (!users.isEmpty()) {
                //매칭 참가 초대를 보낸다
                startMatching(users);
                // 큐에서 참가자들의 매칭 요청을 제거한다
                removeParticipant(users);
                break;
            }
        }

    }


    private List<WaitingUser> selectMatchUsers(Integer matchType, Integer matchTitle) {
        return null;
    }

    private void startMatching(List<WaitingUser> users) {
        String matchId = UUID.randomUUID().toString();

        matchResponses.put(matchId, new ConcurrentHashMap<>());
        for (WaitingUser user : users) {
            template.convertAndSendToUser(
                    user.user, "/user/match/acceptance",
                    Map.of("matchId", matchId)
            );
        }

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            evaluateResponse(matchId, users);
        }, TIMEWAIT, TimeUnit.SECONDS);

    }

    private void evaluateResponse(String matchId, List<WaitingUser> userList) {
        Map<String, Boolean> userResponses = matchResponses.get(matchId);
        int accept = 0, refuse = 0, matchCount = userList.size();
        for (Map.Entry<String, Boolean> e : userResponses.entrySet()) {
            if (e.getValue().equals(ACCEPT))
                accept++;
            else
                refuse++;
        }
        if (accept == matchCount) { //모두 참여하기를 눌렀을 경우 debate 서버에 전송
            requestRoomGenerate();
        } else { // 매칭이 불발 된 경우

            for (Map.Entry<String, Boolean> e : userResponses.entrySet()) {
                if (e.getValue().equals(ACCEPT))
                    accept++;
                else
                    refuse++;
            }
        }
    }


    private void removeParticipant(List<WaitingUser> users) {
        for (List<List<ConcurrentNavigableMap<Long, String>>> typeList : matchQueue) { //타입 (일대일)
            for (List<ConcurrentNavigableMap<Long, String>> titleList : typeList) { //주제
                for (ConcurrentNavigableMap<Long, String> queue : titleList) { //주제에 찬,반, 상관 없음 큐 순회
                    for (WaitingUser user : users)
                        queue.remove(user.timestamp);
                }
            }
        }
    }


    // 참가자들에게 보낸 참가 확인에 대한 답을 matchId에 기록
    // 30초 후에 기록한 내용을 바탕으로 방 생성 or 매칭 취소 결정
    public void receiveMatchAccept(MatchAcceptMessage message, String user) {
        String matchId = message.getMatchId();
        Boolean answer = message.getAccept();
        matchResponses.get(matchId).put(user, answer);

        //매칭 참여자들에게 실시간 수락 정보를 보낸다

    }


    private void requestRoomGenerate() {

    }
}

@Data
@Builder
class WaitingUser {
    Long timestamp; //나노초 단위의 큐 진입시각을 key 값으로 사용
    String user; //사용자 이메일
}
