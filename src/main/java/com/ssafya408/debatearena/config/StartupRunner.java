package com.ssafya408.debatearena.config;

import com.ssafya408.debatearena.service.topic.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupRunner {
  private final TopicService topicService;
  @EventListener(ApplicationReadyEvent.class)
  public void runAfterStartUp() {
    topicService.initiateTopics();
  }
}
