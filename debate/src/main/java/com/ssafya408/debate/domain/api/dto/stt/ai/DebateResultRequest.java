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
@Schema(description = "토론 최종 결과 요청")
public class DebateResultRequest {
    
    @Schema(description = "토론 주제", example = "야구 빠따 vs 단도")
    private String topic;
    
    @Schema(description = "무승부 여부", example = "true")
    private Boolean draw;
    
    @Schema(description = "전체 토론 데이터")
    private Entire entire;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "전체 토론 데이터")
    public static class Entire {
        @Schema(description = "첫 번째 팀 (num1) 데이터")
        private TeamData num1;
        
        @Schema(description = "두 번째 팀 (num2) 데이터")
        private TeamData num2;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "팀 데이터")
    public static class TeamData {
        @Schema(description = "팀 입장/진영", example = "단도가 이긴다")
        private String position;
        
        @Schema(description = "팀의 전체 발언 내용", example = "단도 진영은 빠따에 비해 훨씬 빠른 공격 속도와 치명타 능력이 핵심 강점이라고 본다...")
        private String text;
        
        @Schema(description = "반박 점수", example = "4.2")
        private Double rebuttal_score;
    }
}
