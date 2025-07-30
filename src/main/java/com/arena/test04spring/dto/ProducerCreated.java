package com.arena.test04spring.dto;

import lombok.Data;

@Data
public class ProducerCreated {
    Long roomId;
    String userEmail;
    String type;
    String producerId;
    String kind;
}
