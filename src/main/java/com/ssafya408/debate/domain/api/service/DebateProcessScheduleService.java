package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.model.DebateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebateProcessScheduleService {

    private final SimpMessagingTemplate template;
    private final DebateService debateService;
    @Qualifier("taskScheduler")
    private final TaskScheduler taskScheduler;
    private final Map<Long, DebateManager> debateMap = new ConcurrentHashMap<>();


    public void gameStart(Long debateId) {
        DebateManager debate = debateMap.get(debateId);
        if (debate == null) return;

        startTurn(debateId, debate);
    }

    private void startTurn(Long debateId, DebateManager debate) {
        if (debate.isFinished()) {
            // EnD
            return;
        }

        taskScheduler.schedule(() -> {
            startTurn(debateId, debate);
        }, Instant.now().plusSeconds(30));
    }


}
