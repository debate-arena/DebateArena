package com.ssafya408.debate.domain.api.dto.debate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectTargetResponseDto {
    private String attacker;
    private String defender;
}
