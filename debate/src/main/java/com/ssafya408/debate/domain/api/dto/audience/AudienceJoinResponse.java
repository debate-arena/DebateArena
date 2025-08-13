package com.ssafya408.debate.domain.api.dto.audience;

import com.ssafya408.debate.domain.api.dto.summary.DebateSummaryResponse;
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
@Schema(description = "시청자 입장 응답")
public class AudienceJoinResponse {
    
    @Schema(description = "토론 요약 데이터")
    private DebateSummaryResponse summaryData;
    
    @Schema(description = "의견 데이터")
    private OpinionData opinionData;
    
    @Schema(description = "공방전 데이터")
    private BattleData battleData;
}
