package com.ssafya408.debate.domain.api.dto.debate;

import lombok.Data;

@Data
public class VoteRequestDto {
    String userEmail;
    Integer team;
}
