package com.arena.signaling.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class Participant {
    private String producerUserEmail;
    private String producerId;
    private List<String> transportIds;
    private Long team;
    private Long turn;
    private Map<String,Boolean> consumerConnectedStatus;
    private Map<String,String> consumerIdMap;

    @JsonProperty
    private Boolean isProducerConnected;
}