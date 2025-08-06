package com.arena.signaling.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatedTransportRequestDto {
    private Long roomId;
    private String userEmail;
    private Boolean isProducer;
    private String producerUserEmail; // isProducer == false일 때만 존재
    private String sessionId;
}
