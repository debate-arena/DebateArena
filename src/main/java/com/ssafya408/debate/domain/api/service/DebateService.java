package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.stt.BattleSTTRequest;
import com.ssafya408.debate.domain.api.dto.room.DebateParticipantRequest;
import com.ssafya408.debate.domain.api.dto.stt.STTMessage;
import com.ssafya408.debate.domain.api.dto.stt.OpinionSTTRequest;
import com.ssafya408.debate.domain.api.dto.stt.STTRequest;
import com.ssafya408.debate.domain.api.dto.stt.ai.BroadcastResponse;
import com.ssafya408.debate.domain.db.DebateRoom;
import com.ssafya408.debate.domain.db.DebateRoomRepository;
import com.ssafya408.debate.domain.db.Topic;
import com.ssafya408.debate.domain.db.TopicRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebateService {
  private final DebateRoomRepository debateRoomRepository;
  private final TopicRepository topicRepository;
  private Map<Long, RoomManager> roomInfos;
  private final SimpMessagingTemplate template;

  private Integer OPINION=0;
  private Integer BATTLE=1;


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
  public void broadcastSTTMessage(String user, STTRequest req) {
    String text=req.getText();
    Long roomId = req.getRoomId();
    RoomManager roomManager = roomInfos.get(roomId);

    log.info("STT 메시지 브로드캐스트 시작 - 사용자: {}, 방ID: {}, 텍스트: {}", user, roomId, text);
    
    if (roomManager == null) {
      log.error("방 매니저를 찾을 수 없습니다 - 방ID: {}", roomId);
      return;
    }

    //모든 사용자들에게 STT 내용을 broadcast 한다
    roomManager.broadcastSTTMessage(template, BroadcastResponse.builder().user(user).text(req.getText()).build());
    log.info("STT 메시지 브로드캐스트 완료 - 방ID: {}", roomId);
  }


  // AI 서버에 STT 모음 텍스트 전송
  public void sendTotalTextToAIServer() {

  }

  // 카프카에 텍스트 저장
  public void saveTextToKafka() {
  }

  public void processOpinionSTTMessage(String user, OpinionSTTRequest req) {
    String text=req.getText();
    Long roomId = req.getRoomId();
    RoomManager roomManager = roomInfos.get(roomId);

    log.info("의견 STT 메시지 처리 시작 - 사용자: {}, 방ID: {}, 순서: {}", user, roomId, req.getOrder());

    if (roomManager == null) {
      log.error("방 매니저를 찾을 수 없습니다 - 방ID: {}", roomId);
      return;
    }

    Integer order = req.getOrder();

    STTMessage texts = roomManager.getOpinions().getOrDefault(user,
        STTMessage.initializeSTTMessage(user));
    texts.addText(text);

    log.info("의견 STT 메시지 저장 완료 - 사용자: {}, 현재 누적 텍스트 길이: {}", user, texts.getJoinedText().length());

//    //만약 가장 마지막 텍스트라면 text를 종합하여 AI 쪽에 전송한다
//    if (order.equals(0)) {
//
//    }
  }

  public void processBattleSTTMessage(String user, BattleSTTRequest request) {
    log.info("배틀 STT 메시지 처리 시작 - 사용자: {}, 방ID: {}", user, request.getRoomId());
    // TODO: 배틀 STT 메시지 처리 로직 구현 필요
    log.info("배틀 STT 메시지 처리 완료 - 사용자: {}", user);
  }

  public Long generateDebateRoom(DebateParticipantRequest req) {
    try {
      Topic topic = topicRepository.findById(req.getTopicId())
          .orElseThrow(() -> new IllegalArgumentException("Topic ID " + req.getTopicId() + "가 존재하지 않습니다."));
      
      DebateRoom debateRoom = DebateRoom.generateDebateRoom(topic, req.getMatchType());
      DebateRoom created = debateRoomRepository.save(debateRoom);

      RoomManager roomManager = RoomManager.generateRoomManager(
          created.getId(),
          req.getTopicId(),
          req.getFirstTeam(),
          req.getSecondTeam());

      roomInfos.put(created.getId(), roomManager);
      log.info("토론방 생성 완료: roomId={}, topicId={}, matchType={}", 
          created.getId(), req.getTopicId(), req.getMatchType());
      
      return created.getId();
    } catch (Exception e) {
      log.error("토론방 생성 중 오류 발생: {}", e.getMessage(), e);
      throw new RuntimeException("토론방 생성에 실패했습니다: " + e.getMessage(), e);
    }
  }

  // 사용 가능한 Topic 목록 조회
  public List<Topic> getAvailableTopics() {
    return topicRepository.findAll();
  }



//  public void generateDebateRoom(List<String> debaters) {
//  }
}
