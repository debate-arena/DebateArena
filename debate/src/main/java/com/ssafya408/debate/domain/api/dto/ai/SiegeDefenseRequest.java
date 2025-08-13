package com.ssafya408.debate.domain.api.dto.ai;

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
@Schema(description = "공방전(공격/방어) 요약 요청")
public class SiegeDefenseRequest {
    
    @Schema(description = "토론 주제", example = "호랑이와 사자 중 누가 동물의 왕인가")
    private String topic;
    
    @Schema(description = "공방전 내용")
    private Key key;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공방전 키 데이터")
    public static class Key {
        @Schema(description = "공격 내용")
        private Side attack;
        
        @Schema(description = "방어 내용")
        private Side defense;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공방전 한쪽 진영 데이터")
    public static class Side {
        @Schema(description = "사용자 ID", example = "abc123")
        private String user_id;
        
        @Schema(description = "진영/입장", example = "호랑이가 이긴다")
        private String position;
        
        @Schema(description = "발언 내용", example = "욜로 욜로 하다가 골로 간다는 말이 있다...")
        private String text;
    }
}
