package com.ssafya408.matching.common.topic.service;

import com.ssafya408.matching.common.topic.dto.TopicList;

/**
 * 토픽 서비스 공통 인터페이스
 * 다른 프로젝트에서 재사용 가능하도록 설계
 */
public interface TopicService {
    
    /**
     * Redis에서 현재 토픽 목록을 조회합니다.
     * @return TopicList 현재 및 다음 토픽 목록
     */
    TopicList getTopics();
    
    /**
     * Redis에 토픽 목록을 저장합니다.
     * @param key Redis 키
     * @param topics 저장할 토픽 목록
     */
    void put(String key, TopicList topics);
    
    /**
     * Redis에서 토픽 데이터를 삭제합니다.
     */
    void clearTopics();
}