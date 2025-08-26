package com.arena.signaling.dto.request;

import lombok.Data;

@Data
public class CreateRouterRequest {
    private Long roomId;
    private String userEmail;
    private String matchtype;   // Unused
}