package com.ssafya408.matching.domain.common.util;

import com.ssafya408.matching.domain.api.dto.ChoiceDto;
import com.ssafya408.matching.domain.api.dto.MatchApplyRequest;
import com.ssafya408.matching.domain.api.dto.WaitingUser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;

@Component
public class MatchUtil {
    private final Integer TYPE = 2, TITLE = 5, CHOICE = 3;

    public void addMatchApplyRequestToQueue(MatchApplyRequest req
            , List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue
            , String user, Long timestamp) {
        List<ChoiceDto> choiceDtos = req.getChoices();
        // 1. 사용자가 선택한 데이터를 기반으로 큐에 추가
        for (ChoiceDto choiceDto : choiceDtos) {
            Integer matchType = choiceDto.getMatchType();
            Integer matchTitle = choiceDto.getMatchTitle();
            Integer choice = choiceDto.getChoice();

            //1. 사용자 선택을 기반으로 해당하는 큐에 추가
            matchQueue.get(matchType).get(matchTitle).get(choice)
                    .put(timestamp, user);

        }
    }


    public MatchApplyRequest popUserWaitingInfosAtQueue(Long timestamp
            , List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue) {
        List<ChoiceDto> choices = new ArrayList<>();
        for (int i = 0; i < TYPE; i++) { //타입 별로 큐를 관리.
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = matchQueue.get(i);
            for (int j = 0; j < TITLE; j++) { //주제 개수만큼 큐를 가지고 있는다
                List<ConcurrentNavigableMap<Long, String>> titleList = typeList.get(j);
                for (int k = 0; k < CHOICE; k++) {
                    ConcurrentNavigableMap<Long, String> queue = titleList.get(k);
                    String user = queue.remove(timestamp);
                    if (user != null) { //큐에 유저가 존재한다면
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

    public List<List<WaitingUser>> selectMatchCandidates(List<List<List<ConcurrentNavigableMap<Long, String>>>> matchQueue) {
        //각 큐를 돌면서 매칭 되는 후보를 선택
        List<List<WaitingUser>> candidates = new ArrayList<>();
        type:
        for (int type = 0; type <= TYPE; type++) { //타입 (일대일)
            List<List<ConcurrentNavigableMap<Long, String>>> typeList = matchQueue.get(type);
            int player = type + 1;
            int total = player * 2;
            title:
            for (int title = 0; title < TITLE; title++) { //주제
                List<ConcurrentNavigableMap<Long, String>> titleList = typeList.get(title);

                int cnt = 0;
                for (int choice = 0; choice < CHOICE; choice++) {
                    ConcurrentNavigableMap<Long, String> queue = titleList.get(choice);
                    cnt += Math.min(queue.size(), player);
                }

                if (cnt >= total) { //remain 이 0이하면 큐를 잡을 수 있는 상태
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
                                    .build()
                            );
                            userCnt++;
                        }
                        ConcurrentNavigableMap<Long, String> anyQueue = titleList.get(anyQueueIdx);
                        while (userCnt < player && !queue.isEmpty()) {
                            Map.Entry<Long, String> e = anyQueue.pollFirstEntry();
                            users.add(WaitingUser.builder()
                                    .timestamp(e.getKey())
                                    .user(e.getValue())
                                    .build()
                            );
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
}
