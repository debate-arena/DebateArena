package com.arena.signaling.repository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisRepository {

    private static final String KEY_PREFIX = "room";
    private HashOperations<String, String, Object> hashOps;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void init() {
        this.hashOps = redisTemplate.opsForHash();
    }

    public Map<String, Object> getRoom(Long roomId) {
        String redisKey = KEY_PREFIX + ":" + roomId;
        return hashOps.entries(redisKey);
    }

    public void updateField(Long roomId) {
        try {
            String redisKey = KEY_PREFIX + ":" + roomId;
            hashOps.put(redisKey, "webRTCStatus", "CONNECTED");
            log.debug("[참가자 모두 연결 성공] - roomId: {}, ", roomId);
        } catch (Exception e) {
            log.error("[참가자 모두 연결 실패] - roomId: {}error: {}", roomId, e.getMessage(), e);
        }
    }

}
