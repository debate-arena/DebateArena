package com.ssafya408.debate.domain.api.dto.stt.ai;

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
@Schema(description = "토론 최종 결과 응답")
public class DebateResultResponse {
    
    @Schema(description = "판정 결과")
    private Result result;
    
    @Schema(description = "판정단 설명", 
            example = "대표 청중 #44의 선택 이유:\\n실전 무술에 관심 많은 30대 남성입니다. 단검의 빠른 공격 속도와 치명타 능력이 현실적인 생존 상황에서 훨씬 유리하다고 느꼈어요. 빠따는 위력이 크지만 준비 동작과 체력 소모가 커서 긴박한 싸움에선 단검이 더 효율적일 것 같습니다. 실제 경험과 논리적 분석이 잘 맞아떨어져서 이 입장을 지지했습니다.")
    private String juror_explain;
    
    @Schema(description = "전체 토론 요약")
    private FullSummarize full_summarize;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "판정 결과")
    public static class Result {
        @Schema(description = "승리 팀", example = "num1")
        private String winner;
        
        @Schema(description = "투표 결과")
        private Votes votes;
        
        @Schema(description = "소프트 점수")
        private SoftScores soft_scores;
        
        @Schema(description = "상세 판정 내역 (총 50명의 판정단)")
        private List<JurorDetail> details;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "투표 결과")
    public static class Votes {
        @Schema(description = "num1 팀 득표", example = "26")
        private Integer num1;
        
        @Schema(description = "num2 팀 득표", example = "24")
        private Integer num2;
        
        @Schema(description = "무승부 득표", example = "0")
        private Integer none;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "소프트 점수")
    public static class SoftScores {
        @Schema(description = "num1 팀 점수", example = "26.219")
        private Double num1;
        
        @Schema(description = "num2 팀 점수", example = "23.781")
        private Double num2;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "판정단 상세 내역")
    public static class JurorDetail {
        @Schema(description = "판정단 번호 (0-49)", example = "0")
        private Integer juror;
        
        @Schema(description = "선택한 팀", example = "num1")
        private String vote;
        
        @Schema(description = "num1과의 유사도", example = "0.10478")
        private Double sim1;
        
        @Schema(description = "num2와의 유사도", example = "0.10152")
        private Double sim2;
        
        @Schema(description = "유사도 차이 (sim1 - sim2)", example = "0.00326")
        private Double diff;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "전체 토론 요약")
    public static class FullSummarize {
        @Schema(description = "num1 팀 요약", 
                example = "단도 진영의 주장은 단검이 빠따에 비해 실전 상황에서 더 우위에 있다는 논리적 근거에 집중되어 있다. 첫째, 단검은 빠따보다 공격 속도가 훨씬 빠르며, 한 번의 공격으로도 치명상을 입힐 수 있는 능력이 핵심 강점으로 제시된다...")
        private String num1;
        
        @Schema(description = "num2 팀 요약", 
                example = "야구 빠따가 단도에 비해 우위에 있다는 주장의 핵심 논리는 리치, 즉 공격 거리의 차이에 기반한다. 야구 빠따는 단도보다 훨씬 긴 거리에서 상대를 타격할 수 있으므로...")
        private String num2;
    }
}
