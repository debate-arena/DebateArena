package com.arena.signaling.dto.response;

import lombok.Data;

@Data
public class TransportConnectedResponseDto {
    private String userEmail;
    private String connectedTransportId;
    private Long roomId;
}
