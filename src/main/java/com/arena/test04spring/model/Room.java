package com.arena.test04spring.model;

import com.arena.test04spring.dto.RouterInfo;
import com.arena.test04spring.dto.TransportInfo;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@Builder
public class Room {
    private Long roomId;
    private int matchType;
    private List<WebSocketSession> participants;
}