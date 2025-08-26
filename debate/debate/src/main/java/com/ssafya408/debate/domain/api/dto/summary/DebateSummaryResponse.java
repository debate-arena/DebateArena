package com.ssafya408.debate.domain.api.dto.summary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토론 요약 응답")
public class DebateSummaryResponse {
    
    @Schema(description = "토론방 ID")
    private Long roomId;
    
    @Schema(description = "의견 요약 목록")
    private List<OpinionSummary> opinion;
    
    @Schema(description = "공방전 요약 목록")
    private List<BattleSummary> battle;
    
    @Schema(description = "최종 요약 목록")
    private List<FinalSummary> final_summary;
    
    @Schema(description = "현재 단계", example = "battle")
    private String current_phase;
    
    @Schema(description = "현재 라운드", example = "2")
    private Integer current_round;
    
    @Schema(description = "응답 시간")
    private LocalDateTime timestamp;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "의견 요약")
    public static class OpinionSummary {
        @Schema(description = "단계", example = "opinion")
        private String phase;
        
        @Schema(description = "라운드", example = "1")
        private Integer round;
        
        @Schema(description = "사용자 ID", example = "user123")
        private String user_id;
        
        @Schema(description = "요약 텍스트", example = "야근하며 늦게까지 일하고 집에서 쉬는 것이 출근길 2시간 고생하는 것보다 낫다고 생각한다.")
        private String text;
        
        @Schema(description = "팀", example = "first")
        private String team;
        
        @Schema(description = "생성 시간")
        private String timestamp;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공방전 요약")
    public static class BattleSummary {
        @Schema(description = "단계", example = "battle")
        private String phase;
        
        @Schema(description = "라운드", example = "1")
        private Integer round;
        
        @Schema(description = "공격자 ID", example = "abc123")
        private String attack_id;
        
        @Schema(description = "방어자 ID", example = "abc321")
        private String defense_id;
        
        @Schema(description = "요약 텍스트", example = "공격 내용 : 호랑이는 사자보다 과소비로 파산 위험이 크고...")
        private String text;
        
        @Schema(description = "반박 점수", example = "6")
        private Integer rebuttal_score;
        
        @Schema(description = "공격 팀", example = "first")
        private String attack_team;
        
        @Schema(description = "방어 팀", example = "second")
        private String defense_team;
        
        @Schema(description = "생성 시간")
        private String timestamp;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "최종 요약")
    public static class FinalSummary {
        @Schema(description = "단계", example = "final")
        private String phase;
        
        @Schema(description = "라운드", example = "1")
        private Integer round;
        
        @Schema(description = "승리 팀", example = "num1")
        private String winner;
        
        @Schema(description = "투표 결과")
        private Votes votes;
        
        @Schema(description = "소프트 점수")
        private SoftScores soft_scores;
        
        @Schema(description = "판정단 설명")
        private String juror_explain;
        
        @Schema(description = "전체 토론 요약")
        private FullSummarize full_summarize;
        
        @Schema(description = "생성 시간")
        private String timestamp;
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
    @Schema(description = "전체 토론 요약")
    public static class FullSummarize {
        @Schema(description = "num1 팀 요약")
        private String num1;
        
        @Schema(description = "num2 팀 요약")
        private String num2;
    }
}
