package com.arena.test04spring.dto;

import lombok.Data;

@Data
public class ConsumerInfo {
    private String producerUserEmail;
    private String consumerId;
    private String producerId;
    private Object rtpParameters;
    private String kind;
    private String userEmail;
}
