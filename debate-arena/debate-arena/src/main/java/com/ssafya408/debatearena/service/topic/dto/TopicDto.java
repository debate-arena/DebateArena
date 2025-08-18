package com.ssafya408.debatearena.service.topic.dto;

import com.ssafya408.debatearena.service.topic.db.Topic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicDto {
    private Long id;
    private String topicText;
    private String firstOption;
    private String secondOption;
    
    // Entity에서 DTO로 변환하는 정적 메서드
    public static TopicDto fromEntity(Topic topic) {
        return TopicDto.builder()
                .id(topic.getId())
                .topicText(topic.getTopicText())
                .firstOption(topic.getFirstOption())
                .secondOption(topic.getSecondOption())
                .build();
    }
}