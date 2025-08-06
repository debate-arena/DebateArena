package com.ssafya408.debate.domain.db.cache;

import com.ssafya408.debate.domain.api.dto.room.RoomStatus;
import com.ssafya408.debate.domain.api.dto.room.WebRTCStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DebateRedisInfo {
  private Long roomId;
  private Integer type;
  
  //주제 및 선택지 
  private Long topicId;
  private String topicText;
  private String firstOption;
  private String secondOption;
  
  // 방 상태
  private RoomStatus status;
  private WebRTCStatus WebRTCStatus;

  private Map<String, String> orders = new HashMap<>();
}
