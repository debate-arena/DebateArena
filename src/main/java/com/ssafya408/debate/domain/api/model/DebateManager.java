package com.ssafya408.debate.domain.api.model;

import java.util.List;

public class DebateManager {
    private Long id;
    private List<Participant> participants;
    private int currentIndex = 0; // 현재 발언자 index

    public Participant getCurrentParticipant() {
        return participants.get(currentIndex);
    }

    public void moveToNextParticipant() {
        currentIndex = (currentIndex + 1) % participants.size();
    }

    public boolean isFinished() {
        return participants.stream().allMatch(p -> p.getRemainingTurns() == 0);
    }
}
