package com.ssafya408.debate.domain.api.dto.control;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaControlInfo {
    private Long roomId;
    private String speaker;
}
