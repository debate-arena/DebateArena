package com.ssafya408.debate.domain.api.dto.debate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.api.dto.room.WebRTCStatus;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpinionStartResponseDto {
    private Long roomId;
    private Integer type;

    private Long topicId;
    private String topicText;
    private String firstOption;
    private String secondOption;

    private RoomStatus status;
    private WebRTCStatus webRTCStatus;

    private List<SpeakerOrder> firstTeam;
    private List<SpeakerOrder> secondTeam;

    private LocalDateTime debateStartAt;  // 추가 필드
}
