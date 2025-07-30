package com.arena.test04spring.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Participant {
    private String producerUserEmail;
    private String producerId;
}
