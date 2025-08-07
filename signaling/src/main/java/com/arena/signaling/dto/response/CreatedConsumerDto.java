package com.arena.signaling.dto.response;

import lombok.Data;

@Data
public class CreatedConsumerDto {
    private String producerUserEmail;
    private String consumerId;
    private String producerId;
    private Object rtpParameters;
    private String kind;
    private String userEmail;
    private Long roomId;
    private String error;
}