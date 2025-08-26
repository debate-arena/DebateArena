package com.ssafya408.debatearena.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 알 수 없는 필드 무시
public class SpeakerOrderDto {
    private Integer order;
    private String speaker;
    private String user; // 추가 필드 (Redis 데이터와 맞춤)
}
