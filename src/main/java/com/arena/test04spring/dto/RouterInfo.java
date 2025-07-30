package com.arena.test04spring.dto;

import com.arena.test04spring.model.Participant;
import lombok.Data;

import java.util.List;

@Data
public class RouterInfo {
    private String userEmail;
    private Long roomId;
    private String id;
    private Object rtpCapabilities;
    private String type;
    private List<Participant> participants;
}
