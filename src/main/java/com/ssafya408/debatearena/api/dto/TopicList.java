package com.ssafya408.debatearena.api.dto;

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
  private List<Long> currentTopics;
  private List<Long> nextTopics;
}
