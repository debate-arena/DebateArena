package com.ssafya408.debatearena.service.topic.dto;

import com.ssafya408.debatearena.service.topic.dto.TopicDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicList {
  private List<TopicDto> currentTopics;
  private List<TopicDto> nextTopics;
}
