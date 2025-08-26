package com.ssafya408.debate.domain.api.dto.stt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "STT 전체 텍스트")
public class STTFullText {
    
    @Schema(description = "사용자")
    private String user;

    @Schema(description = "턴")
    private String turn;

    @Schema(description = "전체 텍스트")
    private String fullText;
    
    @Schema(description = "타임스탬프")
    private String timestamp;
}
