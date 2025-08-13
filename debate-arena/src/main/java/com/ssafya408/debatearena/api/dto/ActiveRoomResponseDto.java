package com.ssafya408.debatearena.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveRoomResponseDto {
    private Long roomId;
    private Integer type;
    private Long topicId;
    private String topicText;
    private String firstOption;
    private String secondOption;
    private String status;
    private String webRTCStatus;
    private List<SpeakerOrderDto> firstTeam;
    private List<SpeakerOrderDto> secondTeam;
    private String createdAt; // LocalDateTime 대신 String 사용
    private Integer participantCount;
    private String matchId;
    
    // 추가 메타데이터
    private String currentPhase;
    private Integer currentTurn;
    private Long timeRemaining; // 남은 시간 (초)
}
