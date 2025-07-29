package com.ssafya408.matching.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptanceStatusDto {
    private String user; // 응답한 사용자
    private Boolean accept; // 수락 여부 (true: 수락, false: 거절)
}
