package com.ssafya408.debate.domain.api.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "공방전 요약 응답")
public class SiegeDefenseResponse {
    
    @Schema(description = "공방전 요약 결과")
    private Result result;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공방전 요약 결과 상세")
    public static class Result {
        @Schema(description = "공격자 ID", example = "abc123")
        private String attack_id;
        
        @Schema(description = "방어자 ID", example = "abc321")
        private String defense_id;
        
        @Schema(description = "공방전 요약 텍스트", 
                example = "공격 내용 : 호랑이는 사자보다 과소비로 파산 위험이 크고, 존재 자체가 소비지향적이라 비판받는다.\\n방어 내용 : 사자는 소비로 경제를 활성화시키며 자본주의에서 긍정적 역할을 한다.\\n반박 점수 : 6점\\n방어는 경제 순환 측면에서 반박했으나, 공격의 본질인 과소비 문제를 완전히 해소하지는 못했다.")
        private String text;
        
        @Schema(description = "반박 점수 (1-10점)", example = "6")
        private Integer rebuttal_score;
    }
}
