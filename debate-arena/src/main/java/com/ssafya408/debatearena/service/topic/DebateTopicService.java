package com.ssafya408.debatearena.service.topic;

import com.ssafya408.debatearena.common.topic.dto.TopicDto;
import com.ssafya408.debatearena.common.topic.dto.TopicList;
import com.ssafya408.debatearena.common.topic.service.TopicServiceImpl;
import com.ssafya408.debatearena.service.topic.db.Topic;
import com.ssafya408.debatearena.service.topic.db.TopicRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Debate Arena 전용 토픽 서비스
 * 공통 TopicServiceImpl을 상속받아 추가 기능 구현
 */
@Service("debateTopicService")
@Slf4j
public class DebateTopicService extends TopicServiceImpl {

  private final TopicRepository topicRepository;

  @Value("${debate.common.topic}")
  private Integer TOPIC;

  @Value("${debate.redis.topic-key}")
  private String CURRENT_TOPICS = "currentTopics";
  public DebateTopicService(RedisTemplate<String, Object> redisTemplate, TopicRepository topicRepository) {
    super(redisTemplate);
    this.topicRepository = topicRepository;
  }

  public void initiateTopics(){
    log.info("[DebateTopicService] initiateTopics 시작");

    int pickCount = TOPIC*2;

    //데이터베이스에서 랜덤 토픽을 가져온다.
    List<Topic> randomTopics = getRandomTopics(pickCount);

    if (randomTopics == null || randomTopics.isEmpty()) {
      log.error("[DebateTopicService] 랜덤 토픽을 가져올 수 없습니다.");
      return;
    }

    List<TopicDto> currentTopics = new ArrayList<>();
    List<TopicDto> nextTopics = new ArrayList<>();

    for (Topic topic : randomTopics) {
      TopicDto topicDto = TopicDto.fromEntity(topic);
      if(currentTopics.size() < TOPIC)
        currentTopics.add(topicDto);
      else
        nextTopics.add(topicDto);
    }

    TopicList topicList = TopicList.builder()
        .currentTopics(currentTopics)
        .nextTopics(nextTopics)
        .build();

    put(CURRENT_TOPICS, topicList);

    log.info("[DebateTopicService] initiateTopics 완료 - 현재: {}, 다음: {}",
        currentTopics.size(), nextTopics.size());
  }

  //주기적으로 주제를 변경
  @Scheduled(cron = "0 0 * * * *")
  public void modifyTopics(){
    log.info("[DebateTopicService] modifyTopics 시작");

    // 한 번만 getTopics() 호출하여 현재 상태 저장
    TopicList currentTopicList = getTopics();
    if (currentTopicList == null) {
      log.warn("[DebateTopicService] 현재 토픽 목록이 없어서 modifyTopics를 실행할 수 없습니다.");
      return;
    }

    //레디스에서 가져온 값을 사용 (next -> current로 이동)
    List<TopicDto> currentTopics = currentTopicList.getNextTopics();

    //이전 주제를 제외한 주제를 다음 주제로 선택
    Set<Long> excludeSet = currentTopicList.getCurrentTopics().stream()
        .map(TopicDto::getId)
        .collect(Collectors.toSet());
    excludeSet.addAll(currentTopics.stream()
        .map(TopicDto::getId)
        .collect(Collectors.toSet()));

    List<TopicDto> nextTopics = new ArrayList<>();

    int pickCount = TOPIC * 3; // 충분한 개수로 설정

    List<Topic> topics = getRandomTopics(pickCount);
    if (topics != null) {
      for (Topic topic : topics) {
        if(nextTopics.size() >= TOPIC)
          break;
        if (!excludeSet.contains(topic.getId())) {
          nextTopics.add(TopicDto.fromEntity(topic));
        }
      }
    }

    // 충분한 nextTopics가 생성되지 않은 경우 처리
    if (nextTopics.size() < TOPIC) {
      log.warn("[DebateTopicService] 다음 토픽이 부족합니다. 필요: {}, 생성: {}", TOPIC, nextTopics.size());
    }

    TopicList newTopicList = TopicList.builder()
        .currentTopics(currentTopics)
        .nextTopics(nextTopics)
        .build();

    put(CURRENT_TOPICS, newTopicList);

    log.info("[DebateTopicService] modifyTopics 완료 - 현재: {}, 다음: {}",
        currentTopics.size(), nextTopics.size());
  }

  private List<Topic> getRandomTopics(int pickCount) {
    List<Topic> totalTopics = topicRepository.findAll();

    if (totalTopics.isEmpty()) {
      log.warn("[DebateTopicService] 데이터베이스에 토픽이 없습니다.");
      return new ArrayList<>();
    }

    // 필요한 만큼만 가져오거나 전체 가져오기
    if (totalTopics.size() >= pickCount) {
      // 매번 다른 결과를 위해 현재 시간을 시드로 사용
      Collections.shuffle(totalTopics, new java.util.Random());
      return new ArrayList<>(totalTopics.subList(0, pickCount));
    } else {
      log.warn("[DebateTopicService] 요청한 토픽 수({})가 전체 토픽 수({})보다 많습니다.",
          pickCount, totalTopics.size());
      Collections.shuffle(totalTopics, new java.util.Random());
      return new ArrayList<>(totalTopics);
    }
  }
}