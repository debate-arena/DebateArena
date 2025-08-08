package com.ssafya408.debate.domain.api.dto.debate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectTargetRequestDto {
    private Long roomId;
    private String target;
}
