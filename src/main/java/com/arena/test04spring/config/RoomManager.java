package com.arena.test04spring.config;

import com.arena.test04spring.model.Participant;
import com.arena.test04spring.model.Room;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Configuration
@Slf4j
public class RoomManager {

    private final Map<Long, List<Participant>> rooms = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionToRoom = new ConcurrentHashMap<>();

    @Bean
    public Map<Long, List<Participant>> rooms() {
        return rooms;
    }

    public void addParticipant(Long roomId, String sessionId) {
        rooms.computeIfAbsent(roomId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
              .add(Participant.builder().producerUserEmail(sessionId).build());
        sessionToRoom.put(sessionId, roomId);
        log.debug("Added participant {} to room {}", sessionId, roomId);  
    }

    public void removeParticipant(Long roomId, String sessionId) {
        List<Participant> participants = rooms.get(roomId);
        if (participants != null) {
            participants.removeIf(p -> p.getProducerUserEmail().equals(sessionId));
            sessionToRoom.remove(sessionId);
            
            // 방이 비어있으면 제거
            if (participants.isEmpty()) {
                rooms.remove(roomId);
                log.debug("Removed empty room {}", roomId);
            }
        }
    }

    public Long getRoomBySessionId(String sessionId) {
        return sessionToRoom.get(sessionId);
    }

    public Set<String> getParticipants(Long roomId) {
        List<Participant> participants = rooms.get(roomId);
        if (participants != null) {
            return participants.stream()
                    .map(Participant::getProducerUserEmail)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }
}