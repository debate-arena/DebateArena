package com.ssafya408.debatearena.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.debatearena.api.dto.DebateRedisInfo;
import com.ssafya408.debatearena.api.dto.SpeakerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DebateRedisRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String KEY_PREFIX = "room";

    public DebateRedisInfo findByRoomId(Long roomId) {
        try {
            String redisKey = KEY_PREFIX + ":" + roomId;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);

            if (entries.isEmpty()) {
                log.warn("Redis에서 토론방 정보를 찾을 수 없음 - roomId: {}", roomId);
                return null;
            }

            // JSON 문자열 → List<SpeakerOrderDto> (더 안전한 파싱)
            List<SpeakerOrderDto> firstTeam = parseSpeakerOrderList(entries.get("firstTeam"));
            List<SpeakerOrderDto> secondTeam = parseSpeakerOrderList(entries.get("secondTeam"));

            DebateRedisInfo info = DebateRedisInfo.builder()
                .roomId(Long.parseLong(entries.get("roomId").toString()))
                .type(Integer.parseInt(entries.get("type").toString()))
                .topicId(Long.parseLong(entries.get("topicId").toString()))
                .topicText(entries.get("topicText").toString())
                .firstOption(entries.get("firstOption").toString())
                .secondOption(entries.get("secondOption").toString())
                .status(entries.get("status").toString())
                .webRTCStatus(entries.get("webRTCStatus").toString())
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

    private List<SpeakerOrderDto> parseSpeakerOrderList(Object teamData) {
        if (teamData == null) {
            return List.of();
        }
        
        try {
            return objectMapper.readValue(
                teamData.toString(),
                new TypeReference<List<SpeakerOrderDto>>() {}
            );
        } catch (Exception e) {
            log.warn("팀 데이터 파싱 실패: {}, error: {}", teamData, e.getMessage());
            return List.of();
        }
    }

    public Set<String> findAllActiveRoomIds() {
        try {
            String pattern = KEY_PREFIX + ":*";
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("활성 토론방 목록 조회 실패 - error: {}", e.getMessage(), e);
            return Set.of();
        }
    }
}
