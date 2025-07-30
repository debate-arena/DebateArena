package com.arena.test04spring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransportInfo {
    private String type;
    private Long roomId;

    @JsonProperty
    private boolean isProducer;
    private String userEmail;
    private String transportId;
    private Object dtlsParameters;
    private Object iceCandidates;
    private Object iceParameters;
    private String producerUserEmail;
}