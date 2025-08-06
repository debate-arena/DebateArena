package com.ssafya408.debatearena.config;

import com.ssafya408.debatearena.service.topic.DebateTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupRunner {
  private final DebateTopicService debateTopicService;
  @EventListener(ApplicationReadyEvent.class)
  public void runAfterStartUp() {
    debateTopicService.initiateTopics();
  }
}
