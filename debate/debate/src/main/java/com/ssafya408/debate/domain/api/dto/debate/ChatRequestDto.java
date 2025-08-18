package com.ssafya408.debate.domain.api.dto.debate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestDto {
    private Long roomId;
    private String nickname;
    private String message;
}
