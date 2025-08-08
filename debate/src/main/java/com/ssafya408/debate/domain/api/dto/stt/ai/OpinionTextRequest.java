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
@Schema(description = "의견 요약 요청")
public class OpinionTextRequest {
    @Schema(description = "사용자 ID", example = "user123")
    private String user_id;
    
    @Schema(description = "토론 주제", example = "야근 vs 출근길 2시간")
    private String topic;
    
    @Schema(description = "진영/입장", example = "야근")
    private String position;
    
    @Schema(description = "발언 내용", example = "야근을 선택하겠습니다. 차라리 늦게까지 일하고 집에서 푹 쉬는 게 출근길에 오래 고생하는 것보다 낫다고 생각해요.")
    private String text;
}
