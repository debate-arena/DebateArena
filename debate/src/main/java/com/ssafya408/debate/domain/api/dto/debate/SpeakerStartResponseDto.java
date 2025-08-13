package com.ssafya408.debate.domain.api.dto.debate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class SpeakerStartResponseDto {
    private String speaker;
    private LocalDateTime speakerStartAt;
}
