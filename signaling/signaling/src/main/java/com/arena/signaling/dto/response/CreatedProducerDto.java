package com.arena.signaling.dto.response;

import lombok.Data;

@Data
public class CreatedProducerDto {
    Long roomId;
    String userEmail;
    String type;
    String producerId;
    String kind;
    String error;
}
