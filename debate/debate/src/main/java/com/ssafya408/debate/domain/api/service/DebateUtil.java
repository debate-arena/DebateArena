package com.ssafya408.debate.domain.api.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DebateUtil {
  private Map<Long, RoomManager> roomInfos;

  public void initRoomInfos(Map<Long, RoomManager> roomInfos) {
    this.roomInfos=roomInfos;
  }
  public void deleteRoomInInMemory(Long roomId) {
    RoomManager roomManager = roomInfos.get(roomId);
    if(roomManager == null) {
      return;
    }
    roomInfos.remove(roomId);
    log.info("방 삭제 완료 - 방ID: {}", roomId);
  }
}
