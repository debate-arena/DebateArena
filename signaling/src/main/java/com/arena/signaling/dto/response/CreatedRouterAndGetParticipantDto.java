package com.arena.signaling.dto.response;

import com.arena.signaling.model.Participant;
import lombok.Data;

import java.util.List;

@Data
public class CreatedRouterAndGetParticipantDto {
    private String userEmail;
    private Long roomId;
    private String id;
    private Object rtpCapabilities;
    private String type;
    private List<Participant> participants;
}
