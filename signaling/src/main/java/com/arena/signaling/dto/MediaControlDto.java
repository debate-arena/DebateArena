package com.arena.signaling.dto;

import lombok.Data;

@Data
public class MediaControlDto {
    private String speaker;
    private Long roomId;
    private String producerId;
}
