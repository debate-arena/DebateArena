package com.ssafya408.debate.domain.api.dto.debate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VoteStartResponseDto {
    LocalDateTime voteStartAt;
}
