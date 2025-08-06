package com.ssafya408.debate.domain.db.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.debate.domain.api.dto.debate.SpeakerOrder;
import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.api.dto.room.WebRTCStatus;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DebateRedisRepository {
  private final RedisTemplate<String, Object> redisTemplate;
  private HashOperations<String, String, Object> hashOps;
  private static final String KEY_PREFIX = "room";
  private final ObjectMapper objectMapper = new ObjectMapper(); // ✅ 사용 통합

  @PostConstruct
  public void init() {
    this.hashOps = redisTemplate.opsForHash();
  }

  public void save(Long roomId, DebateRedisInfo debateInfo) {
    try {
      String redisKey = KEY_PREFIX + ":" + roomId;

      Map<String, Object> map = new HashMap<>();
      map.put("roomId", debateInfo.getRoomId());
      map.put("type", debateInfo.getType());
      map.put("topicId", debateInfo.getTopicId());
      map.put("topicText", debateInfo.getTopicText());
      map.put("firstOption", debateInfo.getFirstOption());
      map.put("secondOption", debateInfo.getSecondOption());
      map.put("status", debateInfo.getStatus().name());
      map.put("webRTCStatus", debateInfo.getWebRTCStatus().name());

      // ✅ List<SpeakerOrder> → JSON 문자열로 저장
      map.put("firstTeam", objectMapper.writeValueAsString(debateInfo.getFirstTeam()));
      map.put("secondTeam", objectMapper.writeValueAsString(debateInfo.getSecondTeam()));

      hashOps.putAll(redisKey, map);
      redisTemplate.expire(redisKey, Duration.ofHours(24));
      log.debug("Redis 저장 완료 - roomId: {}", roomId);
    } catch (Exception e) {
      log.error("Redis 저장 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
      throw new RuntimeException("토론방 정보 저장에 실패했습니다", e);
    }
  }

  public DebateRedisInfo findByRoomId(Long roomId) {
    try {
      String redisKey = KEY_PREFIX + ":" + roomId;
      Map<String, Object> entries = hashOps.entries(redisKey);

      if (entries.isEmpty()) {
        log.warn("Redis에서 토론방 정보를 찾을 수 없음 - roomId: {}", roomId);
        return null;
      }

      // ✅ JSON 문자열 → List<SpeakerOrder>
      List<SpeakerOrder> firstTeam = objectMapper.readValue(
          entries.get("firstTeam").toString(),
          new TypeReference<>() {}
      );

      List<SpeakerOrder> secondTeam = objectMapper.readValue(
          entries.get("secondTeam").toString(),
          new TypeReference<>() {}
      );

      DebateRedisInfo info = DebateRedisInfo.builder()
          .roomId(Long.parseLong(entries.get("roomId").toString()))
          .type(Integer.parseInt(entries.get("type").toString()))
          .topicId(Long.parseLong(entries.get("topicId").toString()))
          .topicText(entries.get("topicText").toString())
          .firstOption(entries.get("firstOption").toString())
          .secondOption(entries.get("secondOption").toString())
          .status(RoomStatus.valueOf(entries.get("status").toString()))
          .webRTCStatus(WebRTCStatus.valueOf(entries.get("webRTCStatus").toString()))
          .firstTeam(firstTeam)
          .secondTeam(secondTeam)
          .build();

      log.debug("Redis 조회 완료 - roomId: {}", roomId);
      return info;

    } catch (Exception e) {
      log.error("Redis 조회 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
      return null;
    }
  }

  public Boolean delete(String roomId) {
    String redisKey = KEY_PREFIX + ":" + roomId;
    return redisTemplate.delete(redisKey);
  }

  public void updateRoomStatus(Long roomId, RoomStatus status) {
    try {
      String redisKey = KEY_PREFIX + ":" + roomId;
      hashOps.put(redisKey, "status", status.name());
      log.debug("방 상태 업데이트 완료 - roomId: {}, status: {}", roomId, status);
    } catch (Exception e) {
      log.error("방 상태 업데이트 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
    }
  }

  public void updateWebRTCStatus(Long roomId, WebRTCStatus status) {
    try {
      String redisKey = KEY_PREFIX + ":" + roomId;
      hashOps.put(redisKey, "webRTCStatus", status.name());
      log.debug("WebRTC 상태 업데이트 완료 - roomId: {}, status: {}", roomId, status);
    } catch (Exception e) {
      log.error("WebRTC 상태 업데이트 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
    }
  }

  public boolean exists(Long roomId) {
    try {
      String redisKey = KEY_PREFIX + ":" + roomId;
      return redisTemplate.hasKey(redisKey);
    } catch (Exception e) {
      log.error("토론방 존재 여부 확인 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
      return false;
    }
  }

  public java.util.Set<String> findAllActiveRoomIds() {
    try {
      String pattern = KEY_PREFIX + ":*";
      return redisTemplate.keys(pattern);
    } catch (Exception e) {
      log.error("활성 토론방 목록 조회 실패 - error: {}", e.getMessage(), e);
      return java.util.Collections.emptySet();
    }
  }

  public void extendTTL(Long roomId, java.time.Duration duration) {
    try {
      String redisKey = KEY_PREFIX + ":" + roomId;
      redisTemplate.expire(redisKey, duration);
      log.debug("TTL 연장 완료 - roomId: {}, duration: {}", roomId, duration);
    } catch (Exception e) {
      log.error("TTL 연장 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
    }
  }

  public void updateField(Long roomId, String field, Object value) {
    try {
      String redisKey = KEY_PREFIX + ":" + roomId;
      hashOps.put(redisKey, field, value);
      log.debug("필드 업데이트 완료 - roomId: {}, field: {}, value: {}", roomId, field, value);
    } catch (Exception e) {
      log.error("필드 업데이트 실패 - roomId: {}, field: {}, error: {}", roomId, field, e.getMessage(), e);
    }
  }
}
