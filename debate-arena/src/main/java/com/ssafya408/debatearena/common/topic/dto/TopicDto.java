package com.ssafya408.debatearena.common.topic.dto;

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
    
    // Entity에서 DTO로 변환하는 정적 메서드 (제네릭하게 구현)
    public static TopicDto fromEntity(Object topic) {
        try {
            // Reflection을 사용해서 다양한 Topic Entity 타입 지원
            Long id = (Long) topic.getClass().getMethod("getId").invoke(topic);
            String topicText = (String) topic.getClass().getMethod("getTopicText").invoke(topic);
            String firstOption = (String) topic.getClass().getMethod("getFirstOption").invoke(topic);
            String secondOption = (String) topic.getClass().getMethod("getSecondOption").invoke(topic);
            
            return TopicDto.builder()
                    .id(id)
                    .topicText(topicText)
                    .firstOption(firstOption)
                    .secondOption(secondOption)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Entity를 TopicDto로 변환하는 중 오류 발생", e);
        }
    }
}