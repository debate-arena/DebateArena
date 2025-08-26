package com.ssafya408.debate.domain.api.dto.audience;

import com.ssafya408.debate.domain.api.dto.stt.STTFullText;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "의견 데이터")
public class OpinionData {
    
    @Schema(description = "1팀 의견 STT 데이터")
    private List<STTFullText> firstTeamOpinion;
    
    @Schema(description = "2팀 의견 STT 데이터")
    private List<STTFullText> secondTeamOpinion;
}
