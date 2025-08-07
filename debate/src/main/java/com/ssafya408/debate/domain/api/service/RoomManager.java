package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.debate.DebateTurn;
import com.ssafya408.debate.domain.api.dto.debate.STTAttackDefense;
import com.ssafya408.debate.domain.api.dto.stt.STTMessage;
import com.ssafya408.debate.domain.api.dto.stt.STTRequest;
import com.ssafya408.debate.domain.api.dto.stt.ai.BroadcastResponse;
import com.ssafya408.debate.domain.common.dto.ApiResponse;
import com.ssafya408.debate.domain.db.rdb.MatchType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  private int teamSize;
  private List<String> firstTeam;
  private List<String> secondTeam;
  private int currentOpinionIndex = 0;
  private int currentBattleIndex = 0;
  private String status="opinion";
  private DebateTurn turn = DebateTurn.ATTACK; //

  private Map<String, STTMessage> opinions; //각 사용자의 stt 텍스트가 저장됨
  // 공방전 데이터 어떻게?
  private List<STTAttackDefense> firstTeamAttack; //공방전 의견 {질문, 답변} 형식
  private List<STTAttackDefense> secondTeamAttack; //공방전 의견 {질문, 답변} 형식

  private RoomManager(Long roomId, MatchType type, Long topicId,
      List<String> firstTeam,List<String> secondTeam) {
    this.roomId=roomId;
    this.type=type;
    this.topicId=topicId;
    this.firstTeam = firstTeam;
    this.secondTeam = secondTeam;
    playerCount = firstTeam.size() + secondTeam.size();
    this.teamSize = playerCount/2;
    opinions = new TreeMap<>();
    firstTeamAttack = new ArrayList<>(teamSize);
    firstTeamAttack = new ArrayList<>(teamSize);
  }

  public static RoomManager generateRoomManager(Long roomId,MatchType type,Long topicId,
      List<String> firstTeam,
      List<String> secondTeam) {

    return new RoomManager(roomId,type,topicId,firstTeam,secondTeam);
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

  public void saveOpinionText(String user,STTRequest request) {

    STTMessage texts =opinions.getOrDefault(user,
        STTMessage.initializeSTTMessage(user));
    texts.addText(request.getText());
  }

  public void saveBattleText(STTRequest request) {
    int teamIdx = getTeamIdx();
    int orderInTeam = getOrderInTeam();
    STTAttackDefense sttAttackDefense=null;
    if (teamIdx == 0) {
      sttAttackDefense= firstTeamAttack.get(orderInTeam);
    } else {
      sttAttackDefense= secondTeamAttack.get(orderInTeam);
    }


    if (turn.equals(DebateTurn.ATTACK)) {
      sttAttackDefense.addSTTTextAtAttack(request);
    } else {
      sttAttackDefense.addSTTTextAtDefense(request);
    }
  }
  public String getSpeakerTotalOpinion(String user) {
    return opinions.get(user).getJoinedText();
  }
  public String getCurrentSpeaker() {
    int teamIdx = this.getTeamIdx();
    int orderInTeam = this.getOrderInTeam();
    String user= (teamIdx==0)?
        this.firstTeam.get(orderInTeam)
        :this.secondTeam.get(orderInTeam);

    return user;
  }


  public STTAttackDefense getCurrentTotalSTTBattle() {
    int team=getTeamIdx();
    int order=getOrderInTeam();

    return (team==0)?
        firstTeamAttack.get(order)
        : secondTeamAttack.get(order);
  }

  public int getTeamIdx() {
    int currentIndex = getCurrentIndex();
    return currentIndex%teamSize;
  }


  public int getOrderInTeam() {
    int currentIndex = getCurrentIndex();
    return currentIndex/teamSize;
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
