package com.ssafya408.matching.domain.api;

import com.ssafya408.matching.domain.api.dto.ChoiceDto;
import com.ssafya408.matching.domain.api.dto.MatchAcceptMessage;
import com.ssafya408.matching.domain.api.dto.MatchRequestMessage;
import com.ssafya408.matching.domain.api.dto.MatchStatusDto;
import com.ssafya408.matching.domain.api.dto.MatchStatusMessageResponse;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchService {
  //매칭 타입 개수 eg) 1:1 => [0], 2:2 => [1], ...
  //주제 타입 개수 eg) 주제1, 주제2, 주제3...
  //선택지 개수 eg) 1번 선택, 2번 선택, 3번 선택...
  private final Integer TYPE=2, TITLE=5, CHOICE=3;
  private final Integer TIMEWAIT=30; //매칭 초대 수락 대기시간
  private final Integer ACCEPT=0, REFUSE=1;
  private List<List<List<ConcurrentNavigableMap<Long,String>>>> matchQueue; //매치 큐
  private Map<String, Map<String,Boolean>> matchResponses;
  private Set<String> alreadyMatched, canceled;
  private final SimpMessagingTemplate template;

  @PostConstruct
  public void initMatchService() {
    matchQueue = new ArrayList<>();
    for (int i = 0; i < TYPE; i++) { //타입 별로 큐를 관리.
      List<List<ConcurrentNavigableMap<Long,String>>> typeList = new ArrayList<>();
      for (int j = 0; j < TITLE; j++) { //주제 개수만큼 큐를 가지고 있는다
        List<ConcurrentNavigableMap<Long,String> > titleList = new ArrayList<>();
        for(int k=0;k<CHOICE;k++){ //주제별로 3개의 큐를 가지고 있음

          ConcurrentNavigableMap<Long,String> dq = new ConcurrentSkipListMap<>();
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

  public void sendMatchStatus(){
    List<MatchStatusDto> matchStatusDtos = new ArrayList<>();
    for (int i = 0; i < TYPE; i++) { //타입 별로 큐를 관리.
      List<List<ConcurrentNavigableMap<Long,String>>> typeList = matchQueue.get(i);
      for (int j = 0; j < TITLE; j++) { //주제 개수만큼 큐를 가지고 있는다
        List<ConcurrentNavigableMap<Long,String>> titleList = typeList.get(j);
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

    template.convertAndSend("/sub/match/status",matchStatusDtos);
  }

  public void processMatchQueue(String user,MatchRequestMessage message) {

    // 1. 사용자가 선택한 데이터를 기반으로 큐에 추가
    List<ChoiceDto> choiceDtos = message.getChoiceDtos();

    for (ChoiceDto choiceDto : choiceDtos) {
      Integer matchType = choiceDto.getMatchType();
      Integer matchTitle = choiceDto.getMatchTitle();
      Integer choice = choiceDto.getChoice();
      
      //1. 사용자 선택을 기반으로 해당하는 큐에 추가
      matchQueue.get(matchType).get(matchTitle).get(choice)
          .put(System.nanoTime(),user);
      
      //2. 현재 큐를 판단해서 매칭이 가능하다면
      List<Map<String, Integer>> users = matchPossible(matchType, matchTitle);
      if (users.size()>0) {
        //매칭 참가 초대를 보낸다

        // 큐에서 참가자들의 매칭 요청을 제거한다

        break;
      }
    }

  }

  private List<Map<String, Integer>> matchPossible(Integer matchType, Integer matchTitle) {
    return null;
  }

  private void startMatching(List<String> userList) {
    String matchId = UUID.randomUUID().toString();

    matchResponses.put(matchId, new ConcurrentHashMap<>());
    for (String user : userList) {
      template.convertAndSendToUser(
          user, "/user/match/acceptance",
          Map.of("matchId",matchId)
      );
    }

    Executors.newSingleThreadScheduledExecutor().schedule(() -> {
      evaluateResponse(matchId,userList);
    }, TIMEWAIT, TimeUnit.SECONDS);

  }

  private void evaluateResponse(String matchId, List<String> userList) {
    Map<String, Boolean> userResponses = matchResponses.get(matchId);
    int accept = 0, refuse = 0, matchCount = userList.size();
    for (Map.Entry<String, Boolean> e : userResponses.entrySet()) {
      if(e.getValue().equals(ACCEPT))
        accept++;
      else
        refuse++;
    }
    if (accept == userList.size()) {

    }
  }



  // 참가자들에게 보낸 참가 확인에 대한 답을 matchId에 기록
  // 30초 후에 기록한 내용을 바탕으로 방 생성 or 매칭 취소 결정
  public void receiveMatchAccept(MatchAcceptMessage message, String user) {
    String matchId = message.getMatchId();
    Boolean answer = message.getAccept();
    matchResponses.get(matchId).put(user, answer);
  }


  private void requestRoomGenerate() {

  }
}
@Data
@Builder
class MatchPair{
  private Long timestamp; //나노초 단위의 큐 진입시각을 key 값으로 사용
  private String email; //사용자 이메일
}
