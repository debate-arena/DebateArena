package com.ssafya408.matching.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchAcceptRequest {
    private String matchId;
    private String topicId;
    private Integer team; // [0]: 1번, [1]: 2번
    private Boolean accept;
}
