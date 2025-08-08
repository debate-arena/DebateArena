package com.ssafya408.debate.domain.api.service;

import com.ssafya408.debate.domain.api.dto.stt.ai.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    private static final String AI_SERVER_BASE_URL = "http://localhost:8000";

    /**
     * AI 서버에 의견 요약 요청
     * @param request 의견 요약 요청 데이터
     * @return 의견 요약 응답
     */
    public OpinionSummaryResponse requestOpinionSummary(OpinionTextRequest request) {
        log.info("AI 서버 의견 요약 요청 - userId: {}, topic: {}", 
            request.getUser_id(), request.getTopic());
        
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            
            log.debug("의견 요약 요청 JSON: {}", requestJson);
            
            return webClient.post()
                .uri(AI_SERVER_BASE_URL+"/summaries/opinion")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .onStatus(status -> status.value() == 422, 
                    this::handle422Error)
                .bodyToMono(OpinionSummaryResponse.class)
                .block();
                
        } catch (Exception e) {
            log.error("의견 요약 요청 실패 - userId: {}, error: {}", 
                request.getUser_id(), e.getMessage(), e);
            throw new RuntimeException("의견 요약 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * AI 서버에 공방전 요약 요청
     * @param request 공방전 요약 요청 데이터
     * @return 공방전 요약 응답
     */
    public SiegeDefenseResponse requestSiegeDefenseSummary(SiegeDefenseRequest request) {
        log.info("AI 서버 공방전 요약 요청 - topic: {}", request.getTopic());
        
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            
            log.debug("공방전 요약 요청 JSON: {}", requestJson);
            
            return webClient.post()
                .uri(AI_SERVER_BASE_URL+"/summaries/seigedefense")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .onStatus(status -> status.value() == 422, 
                    this::handle422Error)
                .bodyToMono(SiegeDefenseResponse.class)
                .block();
                
        } catch (Exception e) {
            log.error("공방전 요약 요청 실패 - topic: {}, error: {}", 
                request.getTopic(), e.getMessage(), e);
            throw new RuntimeException("공방전 요약 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * AI 서버에 토론 최종 결과 요청
     * @param request 토론 결과 요청 데이터
     * @return 토론 결과 응답
     */
    public DebateResultResponse requestDebateResult(DebateResultRequest request) {
        log.info("AI 서버 토론 최종 결과 요청 - topic: {}, draw: {}", 
            request.getTopic(), request.getDraw());
        
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            
            log.debug("토론 결과 요청 JSON: {}", requestJson);
            
            return webClient.post()
                .uri(AI_SERVER_BASE_URL+"/summaries/result")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .onStatus(status -> status.value() == 422, 
                    this::handle422Error)
                .bodyToMono(DebateResultResponse.class)
                .block();
                
        } catch (Exception e) {
            log.error("토론 결과 요청 실패 - topic: {}, error: {}", 
                request.getTopic(), e.getMessage(), e);
            throw new RuntimeException("토론 결과 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 422 오류 처리 헬퍼 메서드
     * @param clientResponse 클라이언트 응답
     * @return Mono.error
     */
    private Mono<RuntimeException> handle422Error(org.springframework.web.reactive.function.client.ClientResponse clientResponse) {
        log.error("422 오류 발생");
        return clientResponse.bodyToMono(String.class)
            .flatMap(body -> {
                log.error("422 오류 - 응답 본문: {}", body);
                return Mono.error(new RuntimeException("422 Unprocessable Entity: " + body));
            });
    }

    /**
     * AI 서버 연결 상태 확인
     * @return 연결 성공 여부
     */
    public boolean checkAiServerConnection() {
        try {
            webClient.get()
                .uri(AI_SERVER_BASE_URL+"/health") // AI 서버에 health check 엔드포인트가 있다고 가정
                .retrieve()
                .bodyToMono(String.class)
                .block();
            log.info("AI 서버 연결 확인 성공");
            return true;
        } catch (Exception e) {
            log.error("AI 서버 연결 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}
