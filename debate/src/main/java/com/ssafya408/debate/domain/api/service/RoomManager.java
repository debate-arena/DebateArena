package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.debate.DebateTurn;
import com.ssafya408.debate.domain.api.dto.debate.STTAttackDefense;
import com.ssafya408.debate.domain.api.dto.stt.STTMessage;
import com.ssafya408.debate.domain.api.dto.stt.ai.BroadcastResponse;
import com.ssafya408.debate.domain.common.dto.ApiResponse;
import com.ssafya408.debate.domain.db.rdb.MatchType;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import lombok.Getter;
import lombok.Setter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Getter
@Setter
public class RoomManager {

  private Long roomId;
  private Long topicId;
  private MatchType type;
  private int playerCount;
  private List<String> firstTeam;
  private List<String> secondTeam;
  private int currentOpinionIndex = 0;
  private int currentBattleIndex = 0;
  private String status="opinion";
  private DebateTurn turn = DebateTurn.TEAM1; //

  private TreeMap<String, STTMessage> opinions; //각 사용자의 stt 텍스트가 저장됨
  // 공방전 데이터 어떻게?
  private List<STTAttackDefense> battles; //공방전 의견 {질문, 답변} 형식

  private RoomManager(Long roomId, MatchType type, Long topicId,
      List<String> firstTeam,List<String> secondTeam) {
    this.roomId=roomId;
    this.type=type;
    this.topicId=topicId;
    this.firstTeam = firstTeam;
    this.secondTeam = secondTeam;
    playerCount = firstTeam.size() + secondTeam.size();

    opinions = new TreeMap<>();
    battles = new ArrayList<>(playerCount);
    for (int i = 0; i < playerCount; i++) {
      battles = new ArrayList<>();
    }
  }

  public static RoomManager generateRoomManager(Long roomId,MatchType type,Long topicId,
      List<String> firstTeam,
      List<String> secondTeam) {

    return new RoomManager(roomId,type,topicId,firstTeam,secondTeam);
  }


  public void saveAtBuffer(String user, String content, Integer order) {
  }

  public void broadcastSTTMessage(SimpMessagingTemplate template, BroadcastResponse stt) {

    ApiResponse<BroadcastResponse> res = ApiResponse.success(stt);
    for (String user : firstTeam) {
      template.convertAndSendToUser(user,"/queue/stt/broadcast", res);
    }
    for (String user : secondTeam) {
      template.convertAndSendToUser(user,"/queue/stt/broadcast", res);
    }
  }

  public String getCurrentTotalSTTOpinion() {
    int teamIdx= getCurrentIndex()/2;

    String user= (turn.equals(DebateTurn.TEAM1))?
        firstTeam.get(teamIdx)
        : secondTeam.get(teamIdx);

    return opinions.get(user).getJoinedText();
  }

  public STTAttackDefense getCurrentTotalSTTBattle() {
    int currentIndex = getCurrentIndex();
    return battles.get(currentIndex);
  }

  public int getCurrentIndex() {
    if(status.equals("opinion")){
      return currentOpinionIndex;
    }else{
      return currentBattleIndex;
    }
  }

  public boolean isFinished() {
    if (status.equals("opinion")){
      return currentOpinionIndex == playerCount;
    }else{
      return currentBattleIndex == playerCount;
    }
  }
}
