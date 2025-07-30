package com.arena.test04spring.controller;

import com.arena.test04spring.service.MediasoupPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SignalingController {

    private final MediasoupPublisher mediasoupPublisher;

    @MessageMapping("/join")
    public void join(@Payload JsonNode message,SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublisher.createRouter(headerAccessor, message);
        } catch (Exception e) {
            log.error("Error processing join request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/createTransport")
    public void createTransport(@Payload JsonNode message ,SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublisher.createTransport(headerAccessor, message);
        } catch (Exception e) {
            log.error("Error processing create transport request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/connectTransport")
    public void connectTransport(@Payload JsonNode message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublisher.connectTransport(headerAccessor, message);
        } catch (Exception e) {
            log.error("Error processing connect transport request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/createProducer")
    public void createProducer(@Payload JsonNode message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublisher.createProducer(headerAccessor, message);
        } catch (Exception e) {
            log.error("Error processing create producer request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/createConsumer")
    public void createConsumer(@Payload JsonNode message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublisher.createConsumer(headerAccessor, message);
        } catch (Exception e) {
            log.error("Error processing create consumer request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/resume")
    public void resume(@Payload JsonNode message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublisher.resume(headerAccessor, message);
        } catch (Exception e) {
            log.error("Error processing resume request: {}", e.getMessage(), e);
        }
    }
}