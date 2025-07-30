package com.arena.test04spring.config;

import com.arena.test04spring.model.Participant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class SessionManager {

    @Bean
    public Map<String, String> sessionMap() {
        return new ConcurrentHashMap<>();
    }
}