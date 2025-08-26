package com.arena.signaling.dto;

import lombok.Data;

@Data
public class TransportDisconnectedDto {
    private Long roomId;
    private String userEmail;
    private String transportId;
}
