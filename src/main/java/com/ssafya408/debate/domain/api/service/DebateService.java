package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.DebateParticipantRequest;
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
  private Map<Long, RoomManager> roomInfos = new ConcurrentHashMap<>();
  private final SimpMessagingTemplate template;

  @PostConstruct
  public void init() {
    // 테스트용 Topic 데이터가 없으면 생성
    if (topicRepository.count() == 0) {
      createTestTopics();
    }
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

//  public void processSTTMessage(String user, STTRequest request) {
//    Long roomId = request.getRoomId();
//    String content = request.getContent();
//    Integer order = request.getOrder();
//
//    RoomManager roomManager = roomInfos.get(roomId);
//    roomManager.saveAtBuffer(user, content, order);
//
//    //모든 사용자들에게 STT 내용을 broadcast 한다
//    roomManager.broadcastSTTMessage(template,content);
//
//    if (order.equals(0)) { //마지막 문자열이 들어온다면
//      String totalText=roomManager.getTotalSTT(user);
//
//      // AI 서버에 STT 모음 텍스트 전송
//      //sendTotalTextToAIServer(totalText);
//
//      // 카프카에 텍스트 저장
//    }
//  }

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
