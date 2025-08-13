package com.ssafya408.debate.domain.api.dto.debate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class VoteEndResponseDto {
    Map<String, Team> voteInfo;
    VoteResult voteResult;
    LocalDateTime voteEndAt;
}
