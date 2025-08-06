package com.ssafya408.debate.domain.db.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.api.dto.room.WebRTCStatus;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DebateRedisRepository {
  private final RedisTemplate<String,Object> redisTemplate;
  private HashOperations<String,String,Object> hashOps;
  private static final String KEY_PREFIX = "room";

  @PostConstruct
  public void init() {
    this.hashOps= redisTemplate.opsForHash();
  }

  public void save(Long roomId, DebateRedisInfo debateInfo) {
    String redisKey=KEY_PREFIX+":"+roomId;

    ObjectMapper mapper= new ObjectMapper();
    Map<String, String> map = mapper.convertValue(debateInfo, new TypeReference<>() {});
    hashOps.putAll(redisKey,map);
  }

  public DebateRedisInfo findByRoomId(Long roomId) {
    String redisKey = KEY_PREFIX + ":" + roomId;
    Map<String, Object> entries = hashOps.entries(redisKey);
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(entries, DebateRedisInfo.class);
  }

  public Boolean delete(String roomId) {
    String redisKey = KEY_PREFIX + ":" + roomId;
    return redisTemplate.delete(redisKey);
  }

  public void updateRoomStatus(Long roomId,RoomStatus status) {
    String redisKey = KEY_PREFIX + ":" + roomId;
    hashOps.put(redisKey,"status",status);

  }

  public void updateWebRTCStatus(Long roomId, WebRTCStatus status) {
    String redisKey = KEY_PREFIX + ":" + roomId;
    hashOps.put(redisKey,"webRTCStatus",status);

  }

}
