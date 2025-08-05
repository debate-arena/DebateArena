package com.ssafya408.debate.domain.api.model;

public class Participant {
    private Long userId;
    private int remainingTurns = 6;

    public boolean hasTurnsLeft() {
        return remainingTurns > 0;
    }

    public void useTurn() {
        if (remainingTurns > 0) remainingTurns--;
    }

    public int getRemainingTurns() {
        return remainingTurns;
    }
}
