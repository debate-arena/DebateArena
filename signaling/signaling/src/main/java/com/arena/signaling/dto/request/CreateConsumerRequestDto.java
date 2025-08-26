package com.arena.signaling.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConsumerRequestDto {
    private String userEmail;
    private String producerId;
    private Object rtpCapabilities;
    private String transportId;
    private String producerUserEmail;
    private Long roomId;
}
