package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.debate.DebateTurn;
import com.ssafya408.debate.domain.api.dto.debate.STTAttackDefense;
import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.api.dto.stt.STTMessage;
import com.ssafya408.debate.domain.api.dto.stt.STTRequest;
import com.ssafya408.debate.domain.api.dto.stt.BroadcastResponse;
import com.ssafya408.debate.domain.common.dto.ApiResponse;
import com.ssafya408.debate.domain.db.rdb.MatchType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Getter
@Setter
@Slf4j
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
  private RoomStatus status =RoomStatus.OPINION;
  private DebateTurn turn = DebateTurn.ATTACK; //
  private Map<String,String> attackTarget;

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
    firstTeamAttack = new ArrayList<>();
    secondTeamAttack = new ArrayList<>();

    attackTarget=new HashMap<>();

    for (int i = 0; i < teamSize; i++) {
      firstTeamAttack.add(new STTAttackDefense(firstTeam.get(i))) ;
      secondTeamAttack.add(new STTAttackDefense(secondTeam.get(i))) ;
    }

    tempInitialize();

  }

  public void tempInitialize() {
    attackTarget.put("testuser@example.com", "testuser1@example.com");
    attackTarget.put("testuser1@example.com", "testuser@example.com");
  }

  public void setDefenseUsers(Map<String,String> partners) {
    for (int i = 0; i < teamSize; i++) {
      String firstAttacker = firstTeam.get(i);
      String secondAttacker = secondTeam.get(i);

      firstTeamAttack.get(i).setDefenseUser(partners.get(firstAttacker));
      secondTeamAttack.get(i).setDefenseUser(partners.get(secondAttacker));
    }
  }
  public static RoomManager generateRoomManager(Long roomId,MatchType type,Long topicId,
      List<String> firstTeam,
      List<String> secondTeam) {

    log.info("=== RoomManager 생성 시작 ===");
    log.info("📋 토론방 기본 정보:");
    log.info("   - roomId: {}", roomId);
    log.info("   - type: {}", type);
    log.info("   - topicId: {}", topicId);
    log.info("👥 팀 구성:");
    log.info("   - 첫 번째 팀: {}", firstTeam);
    log.info("   - 두 번째 팀: {}", secondTeam);
    
    RoomManager roomManager = new RoomManager(roomId,type,topicId,firstTeam,secondTeam);
    
    log.info("📊 초기 상태:");
    log.info("   - 총 참가자: {}명", roomManager.getPlayerCount());
    log.info("   - 팀 크기: {}명", roomManager.getTeamSize());
    log.info("   - 초기 상태: {}", roomManager.getStatus());
    log.info("   - currentOpinionIndex: {}", roomManager.getCurrentOpinionIndex());
    log.info("   - currentBattleIndex: {}", roomManager.getCurrentBattleIndex());
    log.info("   - 첫 번째 발화자: teamIdx=0, orderInTeam=0, speaker={}", 
        roomManager.getFirstTeam().get(0));
    log.info("=== RoomManager 생성 완료 ===");
    
    return roomManager;
  }

  public void broadcastSTTMessageAtRoom(SimpMessagingTemplate template, BroadcastResponse stt, Long roomId) {
    log.info("STT 메시지 브로드캐스트 시작 - roomId: {}, 발신자: {}, 텍스트: {}", 
        roomId, stt.getUser(), stt.getText());

    ApiResponse<BroadcastResponse> res = ApiResponse.success(stt);
    String destination = String.format("/debate/room/%s/stt", roomId);

    log.debug("첫 번째 팀에게 메시지 전송 - 팀원: {}", firstTeam);
    template.convertAndSend(destination,res);

    log.info("STT 메시지 브로드캐스트 완료 - 총 {}명에게 전송", playerCount);
  }

  public void saveOpinionText(String user,STTRequest request) {
    log.info("의견 텍스트 저장 - roomId: {}, 사용자: {}, 텍스트: {}", roomId, user, request.getText());
    log.debug("현재 상태 - status: {}, currentIndex: {}", status, currentOpinionIndex);

    STTMessage texts = opinions.getOrDefault(user, STTMessage.initializeSTTMessage(user));
    texts.addText(request.getText());
    opinions.put(user, texts);
    
    log.debug("누적 텍스트 길이 - 사용자: {}, 길이: {}자", user, texts.getJoinedText().length());
    log.info("의견 텍스트 저장 완료 - 사용자: {}", user);
  }

  public void saveBattleText(STTRequest request) {
    int teamIdx = getTeamIdx();
    int orderInTeam = getOrderInTeam();
    
    log.info("배틀 텍스트 저장 - roomId: {}, 텍스트: {}", roomId, request.getText());

    STTAttackDefense sttAttackDefense = null;
    if (teamIdx == 0) {
      sttAttackDefense = firstTeamAttack.get(orderInTeam);
      log.debug("첫 번째 팀 배틀 텍스트 - orderInTeam: {}", orderInTeam);
    } else {
      sttAttackDefense = secondTeamAttack.get(orderInTeam);
      log.debug("두 번째 팀 배틀 텍스트 - orderInTeam: {}", orderInTeam);
    }

    if (turn.equals(DebateTurn.ATTACK)) {
      log.debug("공격 텍스트 저장");
      sttAttackDefense.addSTTTextAtAttack(request);
    } else {
      log.debug("방어 텍스트 저장");
      sttAttackDefense.addSTTTextAtDefense(request);
    }
    
    log.info("배틀 텍스트 저장 완료");
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
    // 번갈아가면서 팀이 바뀌도록 수정: 0,1,0,1,0,1...
    int teamIdx = currentIndex % 2;
    log.info("getTeamIdx >>> currentIndex: {}, teamIdx: {}, team size: {}",
        currentIndex, teamIdx, teamSize);
    return teamIdx;
  }


  public int getOrderInTeam() {
    int currentIndex = getCurrentIndex();
    // 각 팀 내에서의 순서: 0번째 사람, 1번째 사람, 2번째 사람...
    // currentIndex가 0,1이면 각 팀의 0번째, currentIndex가 2,3이면 각 팀의 1번째...
    int orderInTeam = currentIndex / 2;
    log.info("getOrderInTeam >>> currentIndex: {}, orderInTeam: {}, team size: {}",
        currentIndex, orderInTeam, teamSize);
    return orderInTeam;
  }
  public int getCurrentIndex() {
    if(status == RoomStatus.OPINION){
      return currentOpinionIndex;
    }else{
      return currentBattleIndex;
    }
  }

  public boolean isFinished() {
    if (status == RoomStatus.OPINION){
      return currentOpinionIndex == playerCount;
    }else{
      return currentBattleIndex == playerCount;
    }
  }

  // Getter 메서드들 추가
  public int getCurrentOpinionIndex() {
    return currentOpinionIndex;
  }
  
  public int getCurrentBattleIndex() {
    return currentBattleIndex;
  }
  
  public int getTeamSize() {
    return teamSize;
  }
  
  public RoomStatus getStatus() {
    return status;
  }

  // 토론 턴 진행 메서드
  public Map<String, Object> advanceTurn() {
    log.info("=== 토론 턴 진행 시작 - roomId: {} ===", roomId);
    log.info("🔍 진행 전 상태 - status: {}, currentOpinionIndex: {}, currentBattleIndex: {}", 
        status, currentOpinionIndex, currentBattleIndex);
    log.info("📊 토론방 정보 - 총 참가자: {}명, 팀 크기: {}명", playerCount, teamSize);
    log.info("👥 첫 번째 팀: {}", firstTeam);
    log.info("👥 두 번째 팀: {}", secondTeam);
    
    // 현재 진행 상황 로그
    int currentTeamIdx = getTeamIdx();
    int currentOrderInTeam = getOrderInTeam();
    String currentSpeaker = getCurrentSpeaker();
    
    log.info("🎯 현재 발화자 정보 - teamIdx: {}, orderInTeam: {}, speaker: {}", 
        currentTeamIdx, currentOrderInTeam, currentSpeaker);
    
    Map<String, Object> result = new HashMap<>();
    
    if (status == RoomStatus.OPINION) {
      // 의견 단계에서 턴 진행
      if (currentOpinionIndex < playerCount) {
        log.info("⏭️ 의견 단계 턴 진행 - currentOpinionIndex: {} → {}", currentOpinionIndex, currentOpinionIndex + 1);
        currentOpinionIndex++;

        
        // 턴 진행 후 새로운 발화자 정보 로그
        int newTeamIdx = getTeamIdx();
        int newOrderInTeam = getOrderInTeam();
        String nextSpeaker = (currentOpinionIndex< playerCount)?  getCurrentSpeaker(): "turn over";
        
        log.info("📈 턴 진행 후 상태 - currentOpinionIndex: {}/{}", currentOpinionIndex, playerCount);
        log.info("🎯 다음 발화자 정보 - teamIdx: {}, orderInTeam: {}, speaker: {}",
            newTeamIdx, newOrderInTeam, nextSpeaker);
        
        // 의견 단계 완료 체크
        if (currentOpinionIndex >= playerCount) {
          currentBattleIndex = 0;
          log.info("✅ 의견 단계 완료 - 배틀 단계로 전환");
          log.info("🔄 상태 전환: OPINION → BATTLE, currentBattleIndex 초기화: {}", currentBattleIndex);
          result.put("phaseChanged", true);
          result.put("newPhase", "battle");
        }
        
        result.put("currentStatus", status);
        result.put("currentIndex", status == RoomStatus.OPINION ? currentOpinionIndex : currentBattleIndex);
        result.put("isFinished", isFinished());

        
      } else {
        log.warn("❌ 의견 단계가 이미 완료됨 - currentOpinionIndex: {}, playerCount: {}", 
            currentOpinionIndex, playerCount);
        throw new RuntimeException("의견 단계가 이미 완료되었습니다");
      }
      
    } else if (status == RoomStatus.BATTLE) {
      // 배틀 단계에서 턴 진행
      if (currentBattleIndex < playerCount) {
        log.info("⚔️ 배틀 단계 턴 진행 - currentBattleIndex: {} → {}", currentBattleIndex, currentBattleIndex + 1);
        currentBattleIndex++;
        
        // 턴 진행 후 새로운 발화자 정보 로그
        int newTeamIdx = getTeamIdx();
        int newOrderInTeam = getOrderInTeam();
        String nextSpeaker = (currentOpinionIndex< playerCount)?  getCurrentSpeaker(): "turn over";

        log.info("📈 턴 진행 후 상태 - currentBattleIndex: {}/{}", currentBattleIndex, playerCount);
        log.info("🎯 다음 발화자 정보 - teamIdx: {}, orderInTeam: {}, speaker: {}",
            newTeamIdx, newOrderInTeam,nextSpeaker);
        
        // 배틀 단계 완료 체크
        if (currentBattleIndex >= playerCount) {

          status = RoomStatus.FINISH;
          log.info("🏁 배틀 단계 완료 - 토론 종료");
          log.info("🔄 상태 전환: BATTLE → FINISH");
          result.put("debateFinished", true);
        }
        
        result.put("currentStatus", status);
        result.put("currentIndex", currentBattleIndex);
        result.put("isFinished", isFinished());
        
      } else {
        log.warn("❌ 배틀 단계가 이미 완료됨 - currentBattleIndex: {}, playerCount: {}", 
            currentBattleIndex, playerCount);
        throw new RuntimeException("배틀 단계가 이미 완료되었습니다");
      }
      
    } else {
      log.error("알 수 없는 토론 상태: {}", status);
      throw new RuntimeException("알 수 없는 토론 상태: " + status);
    }
    
    // 공통 결과 정보 추가
    result.put("roomId", roomId);
    result.put("message", "턴이 성공적으로 진행되었습니다");
    result.put("timestamp", java.time.LocalDateTime.now().toString());
    
    // 최종 상태 요약 로그
    log.info("📊 턴 진행 완료 요약:");
    log.info("   - 최종 상태: {}", status);
    log.info("   - currentOpinionIndex: {}/{}", currentOpinionIndex, playerCount);
    log.info("   - currentBattleIndex: {}/{}", currentBattleIndex, playerCount);
    log.info("   - 토론 완료 여부: {}", isFinished());
    
    // 현재 상태에 따른 다음 발화자 정보 (토론이 완료되지 않은 경우)
    if (!isFinished()) {
      int finalTeamIdx = getTeamIdx();
      int finalOrderInTeam = getOrderInTeam();
      String finalSpeaker = getCurrentSpeaker();
      log.info("   - 다음 발화자: teamIdx={}, orderInTeam={}, speaker={}", 
          finalTeamIdx, finalOrderInTeam, finalSpeaker);
    }
    
    log.info("=== 토론 턴 진행 완료 - roomId: {} ===", roomId);
    
    return result;
  }
}
