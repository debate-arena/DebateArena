package com.arena.signaling.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class Participant {
    private String producerUserEmail;
    private String producerId;

    @JsonProperty
    private Boolean isProducerConnected;

    private Boolean producerConnectedStatus;
    private Map<String,Boolean> consumerConnectedStatus;

    public void setConsumerConnectedStatus(HashMap<String, Boolean> consumerConnectedStatus) {
    }
}