package com.ssafya408.debate.domain.db.cache;

import com.ssafya408.debate.domain.api.dto.debate.SpeakerOrder;
import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.api.dto.room.WebRTCStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateRedisInfo implements Serializable {
  private Long roomId;
  private Integer type;
  
  //주제 및 선택지 
  private Long topicId;
  private String topicText;
  private String firstOption;
  private String secondOption;
  
  // 방 상태
  private RoomStatus status;
  private WebRTCStatus webRTCStatus;

  // 생성 시간 추가
  private LocalDateTime createdAt;

  private List<SpeakerOrder> firstTeam;
  private List<SpeakerOrder> secondTeam;
}
