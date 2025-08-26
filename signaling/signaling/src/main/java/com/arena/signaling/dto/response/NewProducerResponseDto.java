package com.arena.signaling.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewProducerResponseDto {
    private String producerUserEmail;
    private String producerId;
    private String kind;
}
