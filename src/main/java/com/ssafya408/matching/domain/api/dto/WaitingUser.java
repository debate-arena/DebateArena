package com.ssafya408.matching.domain.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Objects;

@Data
@Builder
public class WaitingUser {
    Long timestamp; //나노초 단위의 큐 진입시각을 key 값으로 사용
    String user; //사용자 이메일

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WaitingUser))
            return false;
        WaitingUser user = (WaitingUser) o;

        return Objects.equals(this, user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.timestamp, user);
    }

    public void givePenalty(Long penalty) {
        this.timestamp += penalty;
    }
}
