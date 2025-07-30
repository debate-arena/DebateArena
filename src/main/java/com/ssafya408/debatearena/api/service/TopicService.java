package com.ssafya408.debatearena.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.debatearena.api.dto.TopicList;
import com.ssafya408.debatearena.db.Topic;
import com.ssafya408.debatearena.db.TopicRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final TopicRepository topicRepository;
  @Value("${debate.common.topic}")
  private Integer TOPIC;

  @Value("${debate.redis.topic-key}")
  private  String CURRENT_TOPICS = "currentTopics";
  public TopicList getTopics() {
    try {
        Object redisData = redisTemplate.opsForValue().get(CURRENT_TOPICS);

      ObjectMapper objectMapper = new ObjectMapper();
      TopicList topicList;

      if (redisData instanceof LinkedHashMap) {
        // RedisTemplate이 JSON을 자동으로 역직렬화하지 못하고 LinkedHashMap으로 반환한 경우
        topicList = objectMapper.convertValue(redisData, TopicList.class);
        String json = objectMapper.writeValueAsString(redisData); // Map -> JSON 문자열 변환
        System.out.println(json);
        log.info("[redis Data]>> {}",json);
      } else if (redisData instanceof String) {
        // Redis에 JSON 문자열로 저장된 경우
        topicList = objectMapper.readValue((String) redisData, TopicList.class);
        log.info("[json Data]>> {}",(String)redisData);

      } else {
        throw new IllegalStateException("Redis 데이터 타입을 처리할 수 없습니다: " + redisData.getClass());
      }
      return topicList;
      
    } catch (Exception e) {
      log.error("[TopicService] Redis에서 토픽 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
      return null;
    }
  }
  
  @SuppressWarnings("unchecked")
  private List<Long> convertToLongList(List<?> list) {
    if (list == null) {
      return new ArrayList<>();
    }
    
    List<Long> result = new ArrayList<>();
    for (Object item : list) {
      if (item instanceof Number) {
        result.add(((Number) item).longValue());
      } else if (item instanceof String) {
        try {
          result.add(Long.parseLong((String) item));
        } catch (NumberFormatException e) {
          log.warn("[TopicService] 숫자로 변환할 수 없는 값: {}", item);
        }
      }
    }
    return result;
  }

  public void put(String key, TopicList topics) {
    redisTemplate.opsForValue().set(key,topics);
  }
  
  public void clearTopics() {
    try {
      redisTemplate.delete(CURRENT_TOPICS);
      log.info("[TopicService] Redis 토픽 데이터 삭제 완료. key: {}", CURRENT_TOPICS);
    } catch (Exception e) {
      log.error("[TopicService] Redis 토픽 데이터 삭제 중 오류 발생: {}", e.getMessage(), e);
      throw e;
    }
  }

  public void initiateTopics(){

    int pickCount = TOPIC*2;

    //레디스에서 가져값을 가져온다.
    List<Topic> randomTopics = getRandomTopics(pickCount);

    List<Long> currentTopics = new ArrayList<>();
    List<Long> nextTopics = new ArrayList<>();

    for (Topic topic : randomTopics) {
      if(currentTopics.size()<TOPIC)
        currentTopics.add(topic.getId());
      else
        nextTopics.add(topic.getId());
    }

    put(CURRENT_TOPICS,TopicList.builder()
        .currentTopics(currentTopics)
        .nextTopics(nextTopics)
        .build()
    );
  }
  
  //주기적으로 주제를 변경
  @Scheduled(cron = "0 0 * * * *")
  public void modifyTopics(){
    log.info("[TopicService] modifyTopics 시작");
    
    // 한 번만 getTopics() 호출하여 현재 상태 저장
    TopicList currentTopicList = getTopics();
    if (currentTopicList == null) {
      log.warn("[TopicService] 현재 토픽 목록이 없어서 modifyTopics를 실행할 수 없습니다.");
      return;
    }
    
    //레디스에서 가져온 값을 사용
    List<Long> currentTopics = currentTopicList.getNextTopics();
    
    //이전 주제를 제외한 주제를 다음 주제로 선택
    Set<Long> excludeSet = new HashSet<>(currentTopicList.getCurrentTopics());
    List<Long> nextTopics = new ArrayList<>();

    int pickCount = TOPIC * 3; // 충분한 개수로 설정

    List<Topic> topics = getRandomTopics(pickCount);
    if (topics != null) {
      for (Topic topic : topics) {
        if(nextTopics.size() >= TOPIC)
          break;
        if (!excludeSet.contains(topic.getId()))
          nextTopics.add(topic.getId());
      }
    }
    
    // 충분한 nextTopics가 생성되지 않은 경우 처리
    if (nextTopics.size() < TOPIC) {
      log.warn("[TopicService] 다음 토픽이 부족합니다. 필요: {}, 생성: {}", TOPIC, nextTopics.size());
    }

    TopicList newTopicList = TopicList.builder()
        .currentTopics(currentTopics)
        .nextTopics(nextTopics)
        .build();
        
    put(CURRENT_TOPICS, newTopicList);
    
    log.info("[TopicService] modifyTopics 완료 - 현재: {}, 다음: {}", 
        currentTopics.size(), nextTopics.size());
  }

  private List<Topic> getRandomTopics(int pickCount) {
    List<Topic> totalTopics = topicRepository.findAll();
    
    if (totalTopics.isEmpty()) {
      log.warn("[TopicService] 데이터베이스에 토픽이 없습니다.");
      return new ArrayList<>();
    }
    
    // 필요한 만큼만 가져오거나 전체 가져오기
    if (totalTopics.size() >= pickCount) {
      // 매번 다른 결과를 위해 현재 시간을 시드로 사용
      Collections.shuffle(totalTopics, new java.util.Random());
      return new ArrayList<>(totalTopics.subList(0, pickCount));
    } else {
      log.warn("[TopicService] 요청한 토픽 수({})가 전체 토픽 수({})보다 많습니다.", 
          pickCount, totalTopics.size());
      Collections.shuffle(totalTopics, new java.util.Random());
      return new ArrayList<>(totalTopics);
    }
  }


}


