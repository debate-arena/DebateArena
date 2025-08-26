package com.arena.signaling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientConnectionEstablishedDto {
    private Long roomId;
    private Boolean isProducer;
    private String consumerUserEmail;
    private String producerUserEmail;
}
