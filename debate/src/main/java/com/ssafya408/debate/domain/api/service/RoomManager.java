package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.debate.DebateTurn;
import com.ssafya408.debate.domain.api.dto.debate.STTAttackDefense;
import com.ssafya408.debate.domain.api.dto.debate.Team;
import com.ssafya408.debate.domain.api.dto.debate.VoteResult;
import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.api.dto.stt.STTMessage;
import com.ssafya408.debate.domain.api.dto.stt.STTRequest;
import com.ssafya408.debate.domain.api.dto.stt.BroadcastResponse;
import com.ssafya408.debate.domain.common.dto.ApiResponse;
import com.ssafya408.debate.domain.common.exception.AudienceException;
import com.ssafya408.debate.domain.db.rdb.MatchType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;
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
  private RoomStatus status =RoomStatus.PREPARING;
  private DebateTurn turn = DebateTurn.ATTACK; //
  private Map<String,String> attackTarget;
  private Map<String, Team> voteTeam;

  private Map<String, STTMessage> opinions; //각 사용자의 stt 텍스트가 저장됨
  // 공방전 데이터 어떻게?
  private List<STTAttackDefense> firstTeamAttack; //공방전 의견 {질문, 답변} 형식
  private List<STTAttackDefense> secondTeamAttack; //공방전 의견 {질문, 답변} 형식
  private Set<String> audiences;

  private Integer MAX_AUDIENCE=20;

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
    audiences=new ConcurrentSkipListSet<>();

//    tempInitialize();

  }

  public void tempInitialize() {
    firstTeamAttack.get(0).setDefenseUser("testuser1@example.com");
    secondTeamAttack.get(0).setDefenseUser("testuser@example.com");
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

  /**
   * 시청자 입장 처리
   * @param audienceId 시청자 ID
   * @throws AudienceException 인원수 초과 또는 중복 입장 시
   */
  public void addAudience(String audienceId) {
    log.info("=== 시청자 입장 요청 처리 시작 ===");
    log.info("시청자: {}, 방ID: {}", audienceId, roomId);
    log.info("현재 시청자 수: {}/{}", audiences.size(), MAX_AUDIENCE);
    
    // 중복 입장 체크
    if (audiences.contains(audienceId)) {
      log.warn("❌ 시청자 중복 입장 시도 - 시청자: {}, 방ID: {}", audienceId, roomId);
      throw new AudienceException("이미 입장한 시청자입니다: " + audienceId);
    }
    
    // 인원수 제한 체크
    if (audiences.size() >= MAX_AUDIENCE) {
      log.warn("❌ 시청자 인원수 초과 - 시청자: {}, 방ID: {}, 현재: {}/{}", 
          audienceId, roomId, audiences.size(), MAX_AUDIENCE);
      throw new AudienceException("시청자 인원수가 초과되었습니다. (최대 " + MAX_AUDIENCE + "명)");
    }
    
    // 시청자 추가
    audiences.add(audienceId);
    log.info("✅ 시청자 입장 성공 - 시청자: {}, 방ID: {}, 현재 시청자 수: {}/{}", 
        audienceId, roomId, audiences.size(), MAX_AUDIENCE);
    log.info("=== 시청자 입장 요청 처리 완료 ===");
  }

  /**
   * 시청자 퇴장 처리
   * @param audienceId 시청자 ID
   */
  public void removeAudience(String audienceId) {
    log.info("=== 시청자 퇴장 처리 시작 ===");
    log.info("시청자: {}, 방ID: {}", audienceId, roomId);
    
    boolean removed = audiences.remove(audienceId);
    if (removed) {
      log.info("✅ 시청자 퇴장 성공 - 시청자: {}, 방ID: {}, 현재 시청자 수: {}/{}", 
          audienceId, roomId, audiences.size(), MAX_AUDIENCE);
    } else {
      log.warn("⚠️ 시청자가 존재하지 않음 - 시청자: {}, 방ID: {}", audienceId, roomId);
    }
    
    log.info("=== 시청자 퇴장 처리 완료 ===");
  }

  /**
   * 현재 시청자 수 조회
   * @return 시청자 수
   */
  public int getAudienceCount() {
    return audiences.size();
  }

  /**
   * 시청자 목록 조회
   * @return 시청자 Set
   */
  public Set<String> getAudiences() {
    return new HashSet<>(audiences);
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
    return opinions.get(user)!=null? opinions.get(user).getJoinedText():"";
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
    return getCurrentIndex() % 2;
  }


  public int getOrderInTeam() {
    int currentIndex = getCurrentIndex();
    int orderInTeam = currentIndex / 2;
    return orderInTeam;
  }
  public int getCurrentIndex() {
    if(status == RoomStatus.OPINION){
      return currentOpinionIndex;
    }else{
      return currentBattleIndex;
    }
  }

  public String getDefender(String attacker) {
    return attackTarget.get(attacker);
  }
  public boolean isFinished() {
    if (status == RoomStatus.BATTLE_VOTE || status == RoomStatus.VOTING)
      return true;
    return false;
  }


  // 토론 턴 진행 메서드
  public Map<String, Object> advanceTurn() {
    Map<String, Object> result = new HashMap<>();
    if(status == RoomStatus.PREPARING) {
      status=RoomStatus.OPINION;
    }
    else if (status == RoomStatus.OPINION) {
      // 의견 단계에서 턴 진행
      if (currentOpinionIndex < playerCount) {
        currentOpinionIndex++;

        // 의견 단계 완료 체크
        if (currentOpinionIndex >= playerCount) {
          status = RoomStatus.BATTLE_VOTE;
          result.put("phaseChanged", true);
          result.put("newPhase", "battle");
        }
        result.put("currentStatus", status);
        result.put("currentIndex", status == RoomStatus.OPINION ? currentOpinionIndex : currentBattleIndex);
        result.put("isFinished", isFinished());
      } else {
        log.info("의견 단계가 이미 완료되었습니다");
//        throw new RuntimeException("의견 단계가 이미 완료되었습니다");
      }
      
    }
    else if(status == RoomStatus.BATTLE_VOTE) {

      status=RoomStatus.BATTLE;
    }
    else if (status == RoomStatus.BATTLE) {
      // 배틀 단계에서 턴 진행
      if (currentBattleIndex < playerCount) {
        currentBattleIndex++;
        // 배틀 단계 완료 체크
        if (currentBattleIndex >= playerCount) {

          status = RoomStatus.VOTING;

          result.put("debateFinished", true);
        }
        
        result.put("currentStatus", status);
        result.put("currentIndex", currentBattleIndex);
        result.put("isFinished", isFinished());
      }

      else {
        log.info("배틀 단계가 이미 완료되었습니다");
//        throw new RuntimeException("배틀 단계가 이미 완료되었습니다");
      }
      
    }
    else if(status == RoomStatus.VOTING) {
      status=RoomStatus.VOTE_RESULT;
    }
    else if(status == RoomStatus.VOTE_RESULT) {
      status=RoomStatus.AI_RESULT;
    }
    else {
      log.error("알 수 없는 토론 상태: {}", status);
//      throw new RuntimeException("알 수 없는 토론 상태: " + status);
    }
    
    // 공통 결과 정보 추가
    result.put("roomId", roomId);
    result.put("message", "턴이 성공적으로 진행되었습니다");
    result.put("timestamp", java.time.LocalDateTime.now().toString());
    
    return result;
  }

  public VoteResult calculateWinner() {
    int firstTeamCount = 0;
    int secondTeamCount = 0;

    if(voteTeam==null){
      return VoteResult.DRAW;
    }

    for (Team team : voteTeam.values()) {
      if (team == null) continue;
      if (team == Team.FIRST_TEAM)
        firstTeamCount++;
      else if (team == Team.SECOND_TEAM)
        secondTeamCount++;
    }

    if (firstTeamCount > secondTeamCount)
      return VoteResult.FIRST_TEAM;
    else if (secondTeamCount > firstTeamCount)
      return VoteResult.SECOND_TEAM;
    else
      return VoteResult.DRAW;
  }
}
