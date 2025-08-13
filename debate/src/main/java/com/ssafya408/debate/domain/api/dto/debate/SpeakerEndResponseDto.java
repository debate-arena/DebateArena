package com.ssafya408.debate.domain.api.dto.debate;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SpeakerEndResponseDto {
    private String speaker;
    private LocalDateTime speakerEndAt;
    private String nextSpeaker;
}
