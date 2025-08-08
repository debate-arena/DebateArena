package com.ssafya408.debate.domain.api.dto.stt.ai;

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
@Schema(description = "의견 요약 응답")
public class OpinionSummaryResponse {
    @Schema(description = "의견 요약 결과")
    private Result result;
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "의견 요약 결과 상세")
    public static class Result {
        @Schema(description = "사용자 ID", example = "user123")
        private String user_id;
        
        @Schema(description = "요약된 의견 텍스트", example = "야근하며 늦게까지 일하고 집에서 쉬는 것이 출근길 2시간 고생하는 것보다 낫다고 생각한다.")
        private String text;
    }
}
