package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.stt.STTMessage;
import com.ssafya408.debate.domain.api.dto.stt.ai.BroadcastResponse;
import com.ssafya408.debate.domain.common.dto.ApiResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Getter
public class RoomManager {

  private Long roomId;
  private Long topicId;
  private int playerCount;
  private List<String> firstTeam;
  private List<String> secondTeam;
  private TreeMap<String, STTMessage> opinions; //각 사용자의 stt 텍스트가 저장됨

  // 공방전 데이터 어떻게?
  private List<Map<String, STTMessage>> battles; //공방전 의견 {질문, 답변} 형식

  private RoomManager(Long roomId, Long topicId,
      List<String> firstTeam,List<String> secondTeam) {
    this.roomId=roomId;
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

  public static RoomManager generateRoomManager(Long roomId,Long topicId,
      List<String> firstTeam,
      List<String> secondTeam) {

    return new RoomManager(roomId,topicId,firstTeam,secondTeam);
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

public String getTotalSTT(String user) {
  return null;
}
}
