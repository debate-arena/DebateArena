package com.ssafya408.debatearena.common.topic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicListWithTimeResponse {
    
    private TopicList topicList;
    private Long timeToNextHour; // 다음 정시까지 남은 시간 (초 단위)
    private String formattedTime; // 포맷된 시간 (예: "25분 30초")
    
}