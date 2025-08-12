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


    private Boolean producerConnectedStatus;
    private Boolean consumerConnectedStatus;

}