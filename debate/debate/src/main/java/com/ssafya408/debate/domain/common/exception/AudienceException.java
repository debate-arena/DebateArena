package com.ssafya408.debate.domain.common.exception;

/**
 * 시청자 입장 관련 예외
 */
public class AudienceException extends RuntimeException {
    
    public AudienceException(String message) {
        super(message);
    }
    
    public AudienceException(String message, Throwable cause) {
        super(message, cause);
    }
}
