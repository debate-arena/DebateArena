package com.ssafya408.debate.domain.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * WebSocket 메시지 처리 중 발생하는 RuntimeException 처리
     * @param ex 발생한 예외
     * @param principal 사용자 정보
     * @return 에러 메시지
     */
    @MessageExceptionHandler(RuntimeException.class)
    @SendToUser("/queue/errors")
    public String handleRuntimeException(RuntimeException ex, Principal principal) {
        String user = principal != null ? principal.getName() : "unknown";
        log.error("WebSocket 메시지 처리 중 RuntimeException 발생 - 사용자: {}, 오류: {}", user, ex.getMessage(), ex);
        
        return "오류가 발생했습니다: " + ex.getMessage();
    }

    /**
     * 일반적인 Exception 처리
     * @param ex 발생한 예외
     * @param principal 사용자 정보
     * @return 에러 메시지
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public String handleException(Exception ex, Principal principal) {
        String user = principal != null ? principal.getName() : "unknown";
        log.error("WebSocket 메시지 처리 중 Exception 발생 - 사용자: {}, 오류: {}", user, ex.getMessage(), ex);
        
        return "시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
    }

    /**
     * 시청자 입장 관련 예외 처리
     * @param ex 발생한 예외
     * @param principal 사용자 정보
     * @return 에러 메시지
     */
    @MessageExceptionHandler(AudienceException.class)
    @SendToUser("/queue/debate/audience/error")
    public String handleAudienceException(AudienceException ex, Principal principal) {
        String user = principal != null ? principal.getName() : "unknown";
        log.error("시청자 입장 관련 예외 발생 - 사용자: {}, 오류: {}", user, ex.getMessage(), ex);
        
        return ex.getMessage();
    }
}
