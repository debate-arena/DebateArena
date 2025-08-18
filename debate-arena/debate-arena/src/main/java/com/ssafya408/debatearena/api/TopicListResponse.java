package com.ssafya408.debatearena.api;

import com.ssafya408.debatearena.service.topic.db.Topic;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicListResponse {
    
    private List<Topic> topics;
    private Long timeToNextHour; // 다음 정각까지 남은 시간 (초 단위)
    private String formattedTime; // 포맷된 시간 (예: "25분 30초")
    
}