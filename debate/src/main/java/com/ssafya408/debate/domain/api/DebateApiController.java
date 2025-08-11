package com.ssafya408.debate.domain.api;

import com.ssafya408.debate.domain.api.dto.debate.SelectTargetRequestDto;
import com.ssafya408.debate.domain.api.dto.stt.OpinionSTTRequest;
import com.ssafya408.debate.domain.api.dto.stt.STTRequest;
import com.ssafya408.debate.domain.api.dto.summary.DebateSummaryResponse;
import com.ssafya408.debate.domain.api.service.DebateService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DebateApiController {

  private final DebateService debateService;
  private final SimpMessagingTemplate messagingTemplate;
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

  @MessageMapping("/debate/{roomId}/audience/join")
  public void joinAsAudience(Principal principal, @DestinationVariable Long roomId) {
    String user = principal.getName();
    log.info("=== 시청자 토론방 입장 요청 수신 시작 ===");
    log.info("시청자: {}, 방ID: {}", user, roomId);

    try {
      log.info("시청자 토론방 입장 처리 시작 - 시청자: {}, 방ID: {}", user, roomId);
      
      // 현재까지의 요약 정보 조회
      DebateSummaryResponse summaryData = debateService.joinAsAudience(user, roomId);
      
      // 시청자에게 요약 정보 전송
      messagingTemplate.convertAndSendToUser(
        user,
        "/queue/debate/audience/summary",
        summaryData
      );
      
      log.info("시청자 토론방 입장 처리 완료 - 시청자: {}, 방ID: {}", user, roomId);
      
    } catch (Exception e) {
      log.error("시청자 토론방 입장 처리 중 오류 발생 - 시청자: {}, 방ID: {}, 오류: {}", 
          user, roomId, e.getMessage(), e);
      
      // 에러 메시지 전송
      messagingTemplate.convertAndSendToUser(
        user,
        "/queue/debate/audience/error",
        "시청자 입장에 실패했습니다: " + e.getMessage()
      );
    }
    
    log.info("=== 시청자 토론방 입장 요청 처리 완료 ===");
  }

  @MessageMapping("/debate/{roomId}/audience/leave")
  public void leaveAsAudience(Principal principal, @DestinationVariable Long roomId) {
    String user = principal.getName();
    log.info("=== 시청자 토론방 퇴장 요청 수신 시작 ===");
    log.info("시청자: {}, 방ID: {}", user, roomId);

    try {
      log.info("시청자 토론방 퇴장 처리 시작 - 시청자: {}, 방ID: {}", user, roomId);
      
      // 시청자 퇴장 처리
      debateService.leaveAsAudience(user, roomId);
      
      log.info("시청자 토론방 퇴장 처리 완료 - 시청자: {}, 방ID: {}", user, roomId);
      
    } catch (Exception e) {
      log.error("시청자 토론방 퇴장 처리 중 오류 발생 - 시청자: {}, 방ID: {}, 오류: {}", 
          user, roomId, e.getMessage(), e);
    }
    
    log.info("=== 시청자 토론방 퇴장 요청 처리 완료 ===");
  }

}
