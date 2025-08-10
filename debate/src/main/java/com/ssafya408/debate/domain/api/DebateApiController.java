package com.ssafya408.debate.domain.api;

import com.ssafya408.debate.domain.api.dto.debate.SelectTargetRequestDto;
import com.ssafya408.debate.domain.api.dto.stt.OpinionSTTRequest;
import com.ssafya408.debate.domain.api.dto.stt.STTRequest;
import com.ssafya408.debate.domain.api.service.DebateService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DebateApiController {

  private final DebateService debateService;
  @MessageMapping("/debate/{roomId}/stt/opinion")
  public void receiveOpinionSTTMessage(Principal principal,@DestinationVariable Long roomId, OpinionSTTRequest request) {
    String user = principal.getName();
    log.info("=== 의견 STT 메시지 수신 시작 ===");
    log.info("사용자: {}, 방ID: {}, 텍스트: {}",
        user, roomId, request.getText());

    try {
      log.info("STT 메시지 브로드캐스트 시작 - 방ID: {}", roomId);
      debateService.broadcastSTTMessage(user,roomId,request);
      log.info("STT 메시지 브로드캐스트 완료");

      log.info("의견 STT 메시지 처리 시작 - 사용자: {}", user);
      debateService.processOpinionSTTMessage(user,roomId, request);
      log.info("의견 STT 메시지 처리 완료");
      
    } catch (Exception e) {
      log.error("의견 STT 메시지 처리 중 오류 발생 - 사용자: {}, 방ID: {}, 오류: {}", 
          user, roomId, e.getMessage(), e);
    }
    
    log.info("=== 의견 STT 메시지 수신 완료 ===");
  }

  @MessageMapping("/debate/{roomId}/stt/battle")
  public void receiveBattleSTTMessage(Principal principal, @DestinationVariable Long roomId,
      STTRequest request) {
    String user = principal.getName();
    log.info("=== 배틀 STT 메시지 수신 시작 ===");
    log.info("사용자: {}, 방ID: {}, 텍스트: {}",
        user, roomId, request.getText());
    try {
      log.info("배틀 STT 메시지 브로드캐스트 시작 - 방ID: {}", roomId);
      debateService.broadcastSTTMessage(user, roomId, request);
      log.info("배틀 STT 메시지 브로드캐스트 완료");

      log.info("배틀 STT 메시지 처리 시작 - 사용자: {}", user);
      debateService.processBattleSTTMessage(user,roomId,request);
      log.info("배틀 STT 메시지 처리 완료");
      
    } catch (Exception e) {
      log.error("배틀 STT 메시지 처리 중 오류 발생 - 사용자: {}, 방ID: {}, 오류: {}", 
          user, roomId, e.getMessage(), e);
    }
    
    log.info("=== 배틀 STT 메시지 수신 완료 ===");
  }

  @MessageMapping("/debate/{roomId}/join")
  public void joinMatch(Principal principal,@DestinationVariable Long roomId) {
    String user = principal.getName();
    log.info("=== 토론방 입장 요청 수신 시작 ===");
    log.info("사용자: {}, 방ID: {}", user, roomId);

    try {
      log.info("토론방 입장 처리 시작 - 사용자: {}, 방ID: {}", user, roomId);
      debateService.userJoinMatch(user, roomId);
      log.info("토론방 입장 처리 완료 - 사용자: {}", user);
      
    } catch (Exception e) {
      log.error("토론방 입장 처리 중 오류 발생 - 사용자: {}, 방ID: {}, 오류: {}", 
          user, roomId, e.getMessage(), e);
    }
    
    log.info("=== 토론방 입장 요청 처리 완료 ===");
  }
  @MessageMapping("/debate/attack")
  public void selectTarget(Principal principal, SelectTargetRequestDto req) {
    String user = principal.getName();
    log.info("타겟 선택 - 사용자: {}, SelectTarget: {}", user, req);

    debateService.selectAttackTarget(user,req);
  }

}
