package com.ssafya408.debate.domain.api.dto.debate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BattleStartResponseDto {
    private String status;
    private LocalDateTime battleStartAt ;
}
