package com.ssafya408.debatearena.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateRedisInfo implements Serializable {
    private Long roomId;
    private Integer type;
    
    //주제 및 선택지 
    private Long topicId;
    private String topicText;
    private String firstOption;
    private String secondOption;
    
    // 방 상태
    private String status;
    private String webRTCStatus;

    private List<SpeakerOrderDto> firstTeam;
    private List<SpeakerOrderDto> secondTeam;
}
