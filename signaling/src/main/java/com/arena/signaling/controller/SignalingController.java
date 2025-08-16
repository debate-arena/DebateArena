package com.arena.signaling.controller;

import com.arena.signaling.dto.request.*;
import com.arena.signaling.service.MediasoupPublishService;
import com.arena.signaling.service.RoomManageService;
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

    private final MediasoupPublishService mediasoupPublishService;
    private final RoomManageService roomManageService;

    @MessageMapping("/join")
    public void join(@Payload CreateRouterRequest req, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            if(!roomManageService.isRoomValid(req.getRoomId())){
                throw new IllegalArgumentException("해당 방이 존재하지 않음");
            }
            mediasoupPublishService.createRouter(principal,headerAccessor, req);
        } catch (Exception e) {
            log.debug("해당 방이 없습니다.");
            log.error("Error processing join request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/createTransport")
    public void createTransport(@Payload CreatedTransportRequestDto req ,Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublishService.createTransport(principal,headerAccessor, req);
        } catch (Exception e) {
            log.error("Error processing create transport request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/connectTransport")
    public void connectTransport(@Payload ConnectTransportRequestDto req,Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublishService.connectTransport(principal,headerAccessor, req);
        } catch (Exception e) {
            log.error("Error processing connect transport request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/createProducer")
    public void createProducer(@Payload CreateProducerRequestDto req,Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublishService.createProducer(principal,headerAccessor, req);
        } catch (Exception e) {
            log.error("Error processing create producer request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/createConsumer")
    public void createConsumer(@Payload CreateConsumerRequestDto req,Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublishService.createConsumer(principal,headerAccessor, req);
        } catch (Exception e) {
            log.error("Error processing create consumer request: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/resume")
    public void resume(@Payload JsonNode message,Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        try {
            mediasoupPublishService.resume(principal,headerAccessor, message);
        } catch (Exception e) {
            log.error("Error processing resume request: {}", e.getMessage(), e);
        }
    }
}