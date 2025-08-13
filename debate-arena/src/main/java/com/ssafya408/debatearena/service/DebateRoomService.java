package com.ssafya408.debatearena.service;

import com.ssafya408.debatearena.api.dto.ActiveRoomResponseDto;
import com.ssafya408.debatearena.api.dto.ActiveRoomsListResponseDto;
import com.ssafya408.debatearena.api.dto.DebateRedisInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebateRoomService {

    private final DebateRedisRepository debateRedisRepository;

    public ActiveRoomsListResponseDto getActiveRoomsWithDetails() {
        try {
            log.info("[활성 토론방 조회] Redis에서 활성 토론방 목록 조회 시작");
            
            Set<String> activeRoomIds = debateRedisRepository.findAllActiveRoomIds();
            List<ActiveRoomResponseDto> activeRooms = new ArrayList<>();
            
            for (String roomIdStr : activeRoomIds) {
                try {
                    // "room:123" 형태에서 "123" 추출
                    Long roomId = Long.parseLong(roomIdStr.replace("room:", ""));
                    DebateRedisInfo roomInfo = debateRedisRepository.findByRoomId(roomId);
                    
                    if (roomInfo != null) {
                        ActiveRoomResponseDto roomDto = convertToActiveRoomResponseDto(roomInfo);
                        activeRooms.add(roomDto);
                    }
                } catch (NumberFormatException e) {
                    log.warn("잘못된 방 ID 형식: {}", roomIdStr);
                } catch (Exception e) {
                    log.error("방 정보 조회 실패 - roomId: {}, error: {}", roomIdStr, e.getMessage());
                }
            }
            
            log.info("[활성 토론방 조회] 성공 - 총 {}개 방", activeRooms.size());
            return ActiveRoomsListResponseDto.of(activeRooms);
            
        } catch (Exception e) {
            log.error("[활성 토론방 조회] 오류 발생: {}", e.getMessage(), e);
            return ActiveRoomsListResponseDto.empty();
        }
    }

    private ActiveRoomResponseDto convertToActiveRoomResponseDto(DebateRedisInfo redisInfo) {
        try {
            return ActiveRoomResponseDto.builder()
                .roomId(redisInfo.getRoomId())
                .type(redisInfo.getType())
                .topicId(redisInfo.getTopicId())
                .topicText(redisInfo.getTopicText())
                .firstOption(redisInfo.getFirstOption())
                .secondOption(redisInfo.getSecondOption())
                .status(redisInfo.getStatus())
                .webRTCStatus(redisInfo.getWebRTCStatus())
                .firstTeam(redisInfo.getFirstTeam())
                .secondTeam(redisInfo.getSecondTeam())
                .createdAt(java.time.LocalDateTime.now().toString()) // String으로 변환
                .participantCount(calculateParticipantCount(redisInfo))
                .build();
                
        } catch (Exception e) {
            log.error("[활성 토론방 조회] 방 정보 변환 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private Integer calculateParticipantCount(DebateRedisInfo redisInfo) {
        int firstTeamCount = redisInfo.getFirstTeam() != null ? redisInfo.getFirstTeam().size() : 0;
        int secondTeamCount = redisInfo.getSecondTeam() != null ? redisInfo.getSecondTeam().size() : 0;
        return firstTeamCount + secondTeamCount;
    }
}
