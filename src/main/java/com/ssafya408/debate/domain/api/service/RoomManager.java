package com.ssafya408.debate.domain.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


public class RoomManager {

  private Long roomId;
  private Long topicId;
  private int playerCount;
  private List<String> firstTeam;
  private List<String> secondTeam;
  private Map<String, List<String>> opinions; //각 사용자의 stt 텍스트가 저장됨

  // 공방전 데이터 어떻게?
  private List<Map<String, List<String>>> sieges;

  private RoomManager(Long roomId, Long topicId, List<String> firstTeam,List<String> secondTeam) {
    this.roomId=roomId;
    this.topicId=topicId;
    this.firstTeam = firstTeam;
    this.secondTeam = secondTeam;
    playerCount = firstTeam.size() + secondTeam.size();
    
    opinions = new ConcurrentHashMap<>();
    sieges = new ArrayList<>(playerCount);
    for (int i = 0; i < playerCount; i++) {
      sieges = new ArrayList<>();
    }
  }

  public static RoomManager generateRoomManager(Long roomId,Long topicId,
      List<String> firstTeam,
      List<String> secondTeam) {

    return new RoomManager(roomId,topicId,firstTeam,secondTeam);
  }


  public void saveAtBuffer(String user, String content, Integer order) {
  }

  public void broadcastSTTMessage(SimpMessagingTemplate template, String content) {
    for (String user : firstTeam) {
      template.convertAndSendToUser(user,"/user/queue/stt", content);
    }for (String user : secondTeam) {
      template.convertAndSendToUser(user,"/user/queue/stt", content);
    }
  }

public String getTotalSTT(String user) {
  return null;
}
}
