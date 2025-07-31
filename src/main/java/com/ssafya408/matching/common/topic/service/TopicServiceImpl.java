package com.ssafya408.matching.common.topic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.matching.common.topic.dto.TopicList;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * TopicService의 기본 구현체
 * Redis를 사용한 토픽 관리 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicServiceImpl implements TopicService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${debate.redis.topic-key:currentTopics}")
    private String CURRENT_TOPICS;

    @Override
    public TopicList getTopics() {
        try {
            Object redisData = redisTemplate.opsForValue().get(CURRENT_TOPICS);
            
            if (redisData == null) {
                log.warn("[TopicService] Redis에서 토픽 데이터를 찾을 수 없습니다. key: {}", CURRENT_TOPICS);
                return null;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            TopicList topicList;

            if (redisData instanceof LinkedHashMap) {
                // RedisTemplate이 JSON을 자동으로 역직렬화하지 못하고 LinkedHashMap으로 반환한 경우
                topicList = objectMapper.convertValue(redisData, TopicList.class);
                String json = objectMapper.writeValueAsString(redisData); // Map -> JSON 문자열 변환
                log.debug("[TopicService] Redis 데이터를 TopicList로 변환: {}", json);
            } else if (redisData instanceof String) {
                // Redis에 JSON 문자열로 저장된 경우
                topicList = objectMapper.readValue((String) redisData, TopicList.class);
                log.debug("[TopicService] JSON 문자열에서 TopicList로 변환: {}", (String)redisData);
            } else if (redisData instanceof TopicList) {
                // 이미 TopicList 타입인 경우
                topicList = (TopicList) redisData;
            } else {
                throw new IllegalStateException("Redis 데이터 타입을 처리할 수 없습니다: " + redisData.getClass());
            }
            
            return topicList;
            
        } catch (Exception e) {
            log.error("[TopicService] Redis에서 토픽 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void put(String key, TopicList topics) {
        try {
            redisTemplate.opsForValue().set(key, topics);
            log.info("[TopicService] Redis에 토픽 데이터 저장 완료. key: {}", key);
        } catch (Exception e) {
            log.error("[TopicService] Redis에 토픽 데이터 저장 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void clearTopics() {
        try {
            redisTemplate.delete(CURRENT_TOPICS);
            log.info("[TopicService] Redis 토픽 데이터 삭제 완료. key: {}", CURRENT_TOPICS);
        } catch (Exception e) {
            log.error("[TopicService] Redis 토픽 데이터 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }
}