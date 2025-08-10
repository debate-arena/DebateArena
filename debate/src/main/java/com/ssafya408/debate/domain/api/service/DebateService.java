package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.debate.STTAttackDefense;
import com.ssafya408.debate.domain.api.dto.debate.SelectTargetRequestDto;
import com.ssafya408.debate.domain.api.dto.debate.SelectTargetResponseDto;
import com.ssafya408.debate.domain.api.dto.debate.SpeakerOrder;
import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.api.dto.room.WebRTCStatus;
import com.ssafya408.debate.domain.api.dto.room.DebateParticipantRequest;
import com.ssafya408.debate.domain.api.dto.stt.OpinionSTTRequest;
import com.ssafya408.debate.domain.api.dto.stt.STTRequest;
import com.ssafya408.debate.domain.api.dto.stt.BroadcastResponse;
import com.ssafya408.debate.domain.db.cache.DebateRedisInfo;
import com.ssafya408.debate.domain.db.cache.DebateRedisRepository;
import com.ssafya408.debate.domain.db.rdb.DebateRoom;
import com.ssafya408.debate.domain.db.rdb.DebateRoomRepository;
import com.ssafya408.debate.domain.db.rdb.MatchType;
import com.ssafya408.debate.domain.db.rdb.Topic;
import com.ssafya408.debate.domain.db.rdb.TopicRepository;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
@RequiredArgsConstructor
@Slf4j
public class DebateService {
  private final DebateRoomRepository debateRoomRepository;
  private final DebateRedisRepository debateRedisRepository;
  private final TopicRepository topicRepository;
  private final RestClient.Builder builder;
  private Map<Long, RoomManager> roomInfos;
  private final SimpMessagingTemplate template;
  private Integer OPINION=0;
  private Integer BATTLE=1;
  // TODO : User Disconnected 시 리소스 해제 처리
  private final Map<Long, Set<String>> beforeGameStartQueue = new ConcurrentHashMap<>();
  private final DebateProcessScheduleService scheduleService;
  @Qualifier("taskScheduler")
  private final TaskScheduler taskScheduler;

  @PostConstruct
  public void init() {
    // 테스트용 Topic 데이터가 없으면 생성
    if (topicRepository.count() == 0) {
      createTestTopics();
    }

    roomInfos = new ConcurrentHashMap<>();
  }

  private void createTestTopics() {
    log.info("테스트용 Topic 데이터를 생성합니다.");
    
    Topic topic1 = new Topic();
    topic1.setTopicText("인공지능의 윤리적 문제");
    topic1.setFirstOption("인공지능 개발을 제한해야 한다");
    topic1.setSecondOption("인공지능 개발을 적극적으로 지원해야 한다");
    topicRepository.save(topic1);

    Topic topic2 = new Topic();
    topic2.setTopicText("소셜미디어의 영향");
    topic2.setFirstOption("소셜미디어는 사회에 긍정적 영향을 준다");
    topic2.setSecondOption("소셜미디어는 사회에 부정적 영향을 준다");
    topicRepository.save(topic2);

    Topic topic3 = new Topic();
    topic3.setTopicText("원격근무의 장단점");
    topic3.setFirstOption("원격근무는 업무 효율성을 높인다");
    topic3.setSecondOption("원격근무는 업무 효율성을 저하시킨다");
    topicRepository.save(topic3);

    log.info("테스트용 Topic 데이터 생성 완료. 총 {}개 생성됨", topicRepository.count());
  }
  public void broadcastSTTMessage(String user, Long  roomId, STTRequest req) {
    String text=req.getText();
    RoomManager roomManager = roomInfos.get(roomId);

    log.info("STT 메시지 브로드캐스트 시작 - 사용자: {}, 방ID: {}, 텍스트: {}", user, roomId, text);
    log.debug("현재 메모리에 있는 방 개수: {}", roomInfos.size());
    log.debug("요청된 방ID {}에 대한 RoomManager 존재 여부: {}", roomId, roomManager != null);
    
    if (roomManager == null) {
      log.error("방 매니저를 찾을 수 없습니다 - 방ID: {}", roomId);
      log.error("현재 활성 방 목록: {}", roomInfos.keySet());
      return;
    }

    //모든 사용자들에게 STT 내용을 broadcast 한다
    BroadcastResponse broadcastResponse = BroadcastResponse.builder().user(user).text(req.getText()).build();
    log.info("브로드캐스트할 메시지 생성 완료 - 사용자: {}, 텍스트: {}", user, text);
    
    roomManager.broadcastSTTMessageAtRoom(template, broadcastResponse,roomId);
    log.info("STT 메시지 브로드캐스트 완료 - 방ID: {}", roomId);
  }

  public void processOpinionSTTMessage(String user, Long roomId, OpinionSTTRequest req) {
    String text=req.getText();
    RoomManager roomManager = roomInfos.get(roomId);

    log.info("의견 STT 메시지 처리 시작 - 사용자: {}, 방ID: {}, 텍스트: {}", user, roomId, text);

    if (roomManager == null) {
      log.error("방 매니저를 찾을 수 없습니다 - 방ID: {}", roomId);
      log.error("현재 활성 방 목록: {}", roomInfos.keySet());
      return;
    }

    log.info("의견 텍스트 저장 시작 - 사용자: {}", user);
    roomManager.saveOpinionText(user,req);
    
    String totalOpinion = roomManager.getSpeakerTotalOpinion(user);
    log.info("의견 STT 메시지 저장 완료 - 사용자: {}", user);
    log.debug("현재 누적된 의견 텍스트 - 사용자: {}, 텍스트: {}", user, totalOpinion);
    log.debug("현재 방의 참가자 수: {}", roomManager.getPlayerCount());
  }



  public void processBattleSTTMessage(String user, Long roomId, STTRequest request) {
    RoomManager roomManager = roomInfos.get(roomId);
    
    log.info("배틀 STT 메시지 처리 시작 - 사용자: {}, 방ID: {}, 텍스트: {}", user, roomId, request.getText());

    if (roomManager == null) {
      log.error("방 매니저를 찾을 수 없습니다 - 방ID: {}", roomId);
      log.error("현재 활성 방 목록: {}", roomInfos.keySet());
      return;
    }
    
    log.info("배틀 텍스트 저장 시작 - 사용자: {}", user);
    //텍스트를 현재 사람에 저장한다
    roomManager.saveBattleText(request);
    log.info("배틀 텍스트 저장 완료 - 사용자: {}", user);

  }


  public Long generateDebateRoom(DebateParticipantRequest req) {
    try {
      Topic topic = topicRepository.findById(req.getTopicId())
          .orElseThrow(() -> new IllegalArgumentException("Topic ID " + req.getTopicId() + "가 존재하지 않습니다."));
      
      DebateRoom debateRoom = DebateRoom.generateDebateRoom(topic, req.getMatchType());
      DebateRoom created = debateRoomRepository.save(debateRoom);

      RoomManager roomManager = RoomManager.generateRoomManager(
          created.getId(),
          created.getMathType(),
          req.getTopicId(),
          req.getFirstTeam(),
          req.getSecondTeam());
      roomInfos.put(created.getId(), roomManager);
      
      // Redis에 토론방 정보 저장
      this.saveDebateInfoAtCache(roomManager, topic);
      
      log.info("토론방 생성 완료: roomId={}, topicId={}, matchType={}", 
          created.getId(), req.getTopicId(), req.getMatchType());
      
      return created.getId();
    } catch (Exception e) {
      log.error("토론방 생성 중 오류 발생: {}", e.getMessage(), e);
      throw new RuntimeException("토론방 생성에 실패했습니다: " + e.getMessage(), e);
    }
  }

  private void saveDebateInfoAtCache(RoomManager debateRoom, Topic topic) {
    List<SpeakerOrder> firstTeam=new ArrayList<>();
    List<SpeakerOrder> secondTeam=new ArrayList<>();
    List<String> first = debateRoom.getFirstTeam();
    List<String> second = debateRoom.getSecondTeam();
    for(int i=0;i<first.size();i++){
      firstTeam.add(new SpeakerOrder(i,first.get(i)));
    }

    for(int i=0;i<second.size();i++){
      secondTeam.add(new SpeakerOrder(i,second.get(i)));
    }

    DebateRedisInfo debateRedisInfo = DebateRedisInfo.builder()
        .roomId(debateRoom.getRoomId())
        .type(MatchType.toInteger(debateRoom.getType()))
        .topicId(topic.getId())
        .topicText(topic.getTopicText())
        .firstOption(topic.getFirstOption())
        .secondOption(topic.getSecondOption())
        .status(RoomStatus.CONNECTING)
        .webRTCStatus(WebRTCStatus.CONNECTING)
        .firstTeam(firstTeam)
        .secondTeam(secondTeam)
        .build();

    debateRedisRepository.save(debateRoom.getRoomId(), debateRedisInfo);
  }

  // 사용 가능한 Topic 목록 조회
  public List<Topic> getAvailableTopics() {
    return topicRepository.findAll();
  }

  // Redis에서 토론방 정보 조회
  public DebateRedisInfo getDebateRoomInfo(Long roomId) {
    try {
      return debateRedisRepository.findByRoomId(roomId);
    } catch (Exception e) {
      log.error("토론방 정보 조회 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
      return null;
    }
  }
  public void userJoinMatch(String user, Long roomId) {
    log.info("=== 사용자 토론방 입장 처리 시작 ===");
    log.info("입장 요청 - 사용자: {}, 방ID: {}", user, roomId);
    
    // TODO : 입장 전 권한 체크하는 로직 (Redis) 구현 필요
    log.debug("입장 대기 큐에 사용자 추가 - 사용자: {}", user);
    
    Set<String> roomQueue = beforeGameStartQueue.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet());
    roomQueue.add(user);
    
    log.info("현재 입장 대기 중인 사용자 수: {}", roomQueue.size());
    log.debug("대기 중인 사용자 목록: {}", roomQueue);
    
    RoomManager roomManager = roomInfos.get(roomId);
    if (roomManager == null) {
      log.error("방 매니저를 찾을 수 없습니다 - 방ID: {}", roomId);
      log.error("현재 활성 방 목록: {}", roomInfos.keySet());
      return;
    }
    
    int expectedPlayerCount = roomManager.getPlayerCount();
    int currentJoinedCount = roomQueue.size();
    
    log.info("토론방 입장 현황 - 예상 참가자: {}, 현재 입장: {}", expectedPlayerCount, currentJoinedCount);

    if (expectedPlayerCount == currentJoinedCount) {
      log.info("=== 모든 참가자 입장 완료 - 토론 시작 ===");
      // TODO : Redis에서 WebRTCStatue 확인
      log.info("토론 게임 시작 스케줄링 - 방ID: {}", roomId);
      scheduleService.gameStart(roomManager);
    } else {
      log.info("토론 시작 대기 중 - 추가로 {}명의 참가자가 필요합니다", expectedPlayerCount - currentJoinedCount);
      // TODO : 게임 시작하면 beforeGameStartQueue 삭제
    }

    log.info("사용자 {}가 방 {}에 참여했습니다.", user, roomId);
    log.info("=== 사용자 토론방 입장 처리 완료 ===");
  }


  // 토론방 상태 업데이트
  public void updateRoomStatus(Long roomId, RoomStatus status) {
    try {
      debateRedisRepository.updateRoomStatus(roomId, status);
      log.info("토론방 상태 업데이트 - roomId: {}, status: {}", roomId, status);
    } catch (Exception e) {
      log.error("토론방 상태 업데이트 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
    }
  }

  // WebRTC 상태 업데이트
  public void updateWebRTCStatus(Long roomId, WebRTCStatus status) {
    try {
      debateRedisRepository.updateWebRTCStatus(roomId, status);
      log.info("WebRTC 상태 업데이트 - roomId: {}, status: {}", roomId, status);
    } catch (Exception e) {
      log.error("WebRTC 상태 업데이트 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
    }
  }

  // 토론방 존재 여부 확인
  public boolean isRoomExists(Long roomId) {
    return debateRedisRepository.exists(roomId);
  }

  // 모든 활성 토론방 조회
  public java.util.Set<String> getActiveRoomIds() {
    return debateRedisRepository.findAllActiveRoomIds();
  }

  // 토론방 TTL 연장 (토론이 진행 중일 때)
  public void extendRoomTTL(Long roomId) {
    debateRedisRepository.extendTTL(roomId, java.time.Duration.ofHours(2));
    log.info("토론방 TTL 연장 - roomId: {}", roomId);
  }

  // 토론방 종료 시 정리
  public void closeDebateRoom(Long roomId) {
    try {
      // 메모리에서 제거
      roomInfos.remove(roomId);
      
      // Redis에서 제거 (또는 상태만 변경)
      updateRoomStatus(roomId, RoomStatus.FINISH);
      
      log.info("토론방 종료 - roomId: {}", roomId);
    } catch (Exception e) {
      log.error("토론방 종료 처리 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
    }
  }

  // 토론 턴 진행 (currentOpinionIndex 또는 currentBattleIndex 증가)
  public Map<String, Object> advanceDebateTurn(Long roomId) {
    log.info("=== 토론 턴 진행 시작 ===");
    log.info("요청된 방ID: {}", roomId);
    
    RoomManager roomManager = roomInfos.get(roomId);
    if (roomManager == null) {
      log.error("방 매니저를 찾을 수 없습니다 - 방ID: {}", roomId);
      log.error("현재 활성 방 목록: {}", roomInfos.keySet());
      throw new RuntimeException("토론방을 찾을 수 없습니다: " + roomId);
    }

    String currentSpeaker = roomManager.getCurrentSpeaker();
    log.info("발화자 >>> {}", currentSpeaker);

    if (roomManager.getStatus() == RoomStatus.OPINION) {
      String speakerTotalOpinion = roomManager.getSpeakerTotalOpinion(currentSpeaker);
      log.info("턴 종료 | 발화자 전체 내용: {}", speakerTotalOpinion);

    } else {
      STTAttackDefense battleContent = roomManager.getCurrentTotalSTTBattle();
      log.info("배틀 STT 메시지 종합 시작 - 공격자 발화 >>> {}, 방어자 발화: {}", battleContent.getAttack(), battleContent.getDefense());

    }
    return roomManager.advanceTurn();
  }

  // 토론 턴 초기화 (테스트용): 상태 OPINION으로, 인덱스 0으로 리셋
  public Map<String, Object> resetDebateTurn(Long roomId) {
    log.info("=== 토론 턴 초기화 시작 ===");
    log.info("요청된 방ID: {}", roomId);

    RoomManager roomManager = roomInfos.get(roomId);
    if (roomManager == null) {
      log.error("방 매니저를 찾을 수 없습니다 - 방ID: {}", roomId);
      log.error("현재 활성 방 목록: {}", roomInfos.keySet());
      throw new RuntimeException("토론방을 찾을 수 없습니다: " + roomId);
    }

    roomManager.setStatus(RoomStatus.OPINION);
    roomManager.setCurrentOpinionIndex(0);
    roomManager.setCurrentBattleIndex(0);

    Map<String, Object> result = new HashMap<>();
    result.put("roomId", roomId);
    result.put("currentStatus", roomManager.getStatus());
    result.put("currentOpinionIndex", roomManager.getCurrentOpinionIndex());
    result.put("currentBattleIndex", roomManager.getCurrentBattleIndex());
    result.put("message", "턴이 초기화되었습니다");
    result.put("timestamp", java.time.LocalDateTime.now().toString());

    log.info("토론 턴 초기화 완료 - roomId: {}, status: {}, opinionIndex: {}, battleIndex: {}",
        roomId, roomManager.getStatus(), roomManager.getCurrentOpinionIndex(), roomManager.getCurrentBattleIndex());
    return result;
  }
  public void selectAttackTarget(String user, SelectTargetRequestDto req) {
    // TODO : 방이 Stage 1,2 사이일때만 공격자 선택을 가능하도록 함
    RoomManager roomManager= roomInfos.get(req.getRoomId());

    if(roomManager == null){
      // TODO : EXCEPTION
      return ;
    }

    Map<String,String> attackTarget = roomManager.getAttackTarget();
    if(attackTarget==null){
      // TODO : EXCEPTION
      attackTarget = new HashMap<>();
    }
    attackTarget.put(user, req.getTarget());
    roomManager.setAttackTarget(attackTarget);

    log.info("[공격자 생성] {} -> {} ",user,req.getTarget());
    log.info("[공격자 생성] {} ",attackTarget);

    SelectTargetResponseDto res = SelectTargetResponseDto.builder()
            .attacker(user)
            .defender(req.getTarget())
            .build();

    template.convertAndSend("/debate/room/"+req.getRoomId()+"/attack",res);
  }

}
