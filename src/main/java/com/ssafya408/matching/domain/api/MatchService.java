package com.ssafya408.matching.domain.api;

import com.ssafya408.matching.domain.api.dto.ChoiceDto;
import com.ssafya408.matching.domain.api.dto.MatchRequestMessage;
import com.ssafya408.matching.domain.api.dto.MatchStatusDto;
import com.ssafya408.matching.domain.api.dto.MatchStatusMessageResponse;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Service;

@Service
public class MatchService {
  private final Integer TYPE=2; //매칭 타입 개수 eg) 1:1 => [0], 2:2 => [1], ...

  private final Integer TITLE=5; //주제 타입 개수 eg) 주제1, 주제2, 주제3...

  private final Integer CHOICE=3;  //선택지 개수 eg) 1번 선택, 2번 선택, 3번 선택...

  private List<List<List<Deque<String>>>> matchQueue; //매치 큐

  private Set<String> alreadyMatched;

  @PostConstruct
  public void initMatchService() {
    matchQueue = new ArrayList<>();
    for (int i = 0; i < TYPE; i++) { //타입 별로 큐를 관리.
      List<List<Deque<String>>> typeList = new ArrayList<>();
      for (int j = 0; j < TITLE; j++) { //주제 개수만큼 큐를 가지고 있는다
        List<Deque<String>> titleList = new ArrayList<>();
        for(int k=0;k<CHOICE;k++){ //주제별로 3개의 큐를 가지고 있음
          Deque<String> dq = new LinkedBlockingDeque<>();
          titleList.add(dq);
        }
        typeList.add(titleList);
      }
      matchQueue.add(typeList);
    }

//    alreadyMatched=new ConcurrentHashSet
  }

  @MessageMapping("/match/status")
  public MatchStatusMessageResponse getMatchStatus(){
    List<MatchStatusDto> matchStatusDtos = new ArrayList<>();
    for (int i = 0; i < TYPE; i++) { //타입 별로 큐를 관리.
      List<List<Deque<String>>> typeList = matchQueue.get(i);
      for (int j = 0; j < TITLE; j++) { //주제 개수만큼 큐를 가지고 있는다
        List<Deque<String>> titleList = typeList.get(j);
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

    return MatchStatusMessageResponse
        .builder()
        .matchStatusDtos(matchStatusDtos)
        .build();
  }

  @MessageMapping("/match/request")
  public void processMatchRequest(MatchRequestMessage message) {
    String user = message.getSender();

    //사용자가 선택한 데이터를 기반으로 큐에 추가
    addToMatchQueue(message);

    //
    

  }

  private void addToMatchQueue(MatchRequestMessage message) {
    String user = message.getSender();
    List<ChoiceDto> choiceDtos = message.getChoiceDtos();

    for (ChoiceDto choiceDto : choiceDtos) {
      Integer matchType = choiceDto.getMatchType();
      Integer matchTitle = choiceDto.getMatchTitle();
      Integer choice = choiceDto.getChoice();

      matchQueue.get(matchType).get(matchTitle).get(choice).addLast(user);
    }
  }

}
