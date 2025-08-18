package com.ssafya408.debate.domain.db.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.debate.domain.api.dto.ai.DebateResultResponse;
import com.ssafya408.debate.domain.api.dto.ai.OpinionSummaryResponse;
import com.ssafya408.debate.domain.api.dto.ai.SiegeDefenseResponse;
import com.ssafya408.debate.domain.api.dto.summary.DebateSummaryResponse;
import com.ssafya408.debate.domain.api.dto.summary.DebateSummaryResponse.BattleSummary;
import com.ssafya408.debate.domain.api.dto.summary.DebateSummaryResponse.FinalSummary;
import com.ssafya408.debate.domain.api.dto.summary.DebateSummaryResponse.OpinionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SummaryRedisRepository {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // 키 접두사
    private static final String SUMMARY_PREFIX = "debate:summary:";
    private static final Duration TTL = Duration.ofHours(1);
    
    /**
     * 의견 요약 저장
     */
    public void saveOpinionSummary(Long roomId, int round, OpinionSummaryResponse response, String team) {
        try {
            String key = SUMMARY_PREFIX + roomId + ":opinion:" + round;
            
            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("phase", "opinion");
            summaryData.put("round", round);
            summaryData.put("user_id", response.getResult().getUser_id());
            summaryData.put("text", response.getResult().getText());
            summaryData.put("team", team);
            summaryData.put("timestamp", LocalDateTime.now().toString());
            
            String summaryJson = objectMapper.writeValueAsString(summaryData);
            redisTemplate.opsForValue().set(key, summaryJson, TTL);
            
            log.info("의견 요약 저장 완료 - roomId: {}, round: {}, userId: {}", roomId, round, response.getResult().getUser_id());
        } catch (Exception e) {
            log.error("의견 요약 저장 실패 - roomId: {}, round: {}, error: {}", roomId, round, e.getMessage(), e);
        }
    }
    
    /**
     * 공방전 요약 저장
     */
    public void saveBattleSummary(Long roomId, int round, SiegeDefenseResponse response,
                                 String attackTeam, String defenseTeam) {
        try {
            String key = SUMMARY_PREFIX + roomId + ":battle:" + round;
            
            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("phase", "battle");
            summaryData.put("round", round);
            summaryData.put("attack_id", response.getResult().getAttack_id());
            summaryData.put("defense_id", response.getResult().getDefense_id());
            summaryData.put("text", response.getResult().getText());
            summaryData.put("rebuttal_score", response.getResult().getRebuttal_score());
            summaryData.put("attack_team", attackTeam);
            summaryData.put("defense_team", defenseTeam);
            summaryData.put("timestamp", LocalDateTime.now().toString());
            
            String summaryJson = objectMapper.writeValueAsString(summaryData);
            redisTemplate.opsForValue().set(key, summaryJson, TTL);
            
            log.info("공방전 요약 저장 완료 - roomId: {}, round: {}, attackId: {}, defenseId: {}", 
                roomId, round, response.getResult().getAttack_id(), response.getResult().getDefense_id());
        } catch (Exception e) {
            log.error("공방전 요약 저장 실패 - roomId: {}, round: {}, error: {}", roomId, round, e.getMessage(), e);
        }
    }
    
    /**
     * 최종 요약 저장
     */
    public void saveFinalSummary(Long roomId, DebateResultResponse response) {
        try {
            String key = SUMMARY_PREFIX + roomId + ":final:1";
            
            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("phase", "final");
            summaryData.put("round", 1);
            summaryData.put("winner", response.getResult().getWinner());
            summaryData.put("votes", response.getResult().getVotes());
            summaryData.put("soft_scores", response.getResult().getSoft_scores());
            summaryData.put("juror_explain", response.getJuror_explain());
            summaryData.put("full_summarize", response.getFull_summarize());
            summaryData.put("timestamp", LocalDateTime.now().toString());
            
            String summaryJson = objectMapper.writeValueAsString(summaryData);
            redisTemplate.opsForValue().set(key, summaryJson, TTL);
            
            log.info("최종 요약 저장 완료 - roomId: {}, winner: {}", roomId, response.getResult().getWinner());
        } catch (Exception e) {
            log.error("최종 요약 저장 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
        }
    }
    
    /**
     * 방의 모든 요약 조회 (시청자 입장용)
     */
    public DebateSummaryResponse getAllSummaries(Long roomId) {
        try {
            // 의견 요약 조회
            List<OpinionSummary> opinionSummaries = getOpinionSummaries(roomId);
            
            // 공방전 요약 조회
            List<BattleSummary> battleSummaries = getBattleSummaries(roomId);
            
            // 최종 요약 조회
            List<FinalSummary> finalSummaries = getFinalSummaries(roomId);
            
            // 현재 단계 정보
            String currentPhase = getCurrentPhase(roomId);
            Integer currentRound = getCurrentRound(roomId);
            
            DebateSummaryResponse result = DebateSummaryResponse.builder()
                .roomId(roomId)
                .opinion(opinionSummaries)
                .battle(battleSummaries)
                .final_summary(finalSummaries)
                .current_phase(currentPhase)
                .current_round(currentRound)
                .timestamp(LocalDateTime.now())
                .build();
            
            log.info("전체 요약 조회 완료 - roomId: {}, 의견: {}개, 공방전: {}개, 최종: {}개", 
                roomId, opinionSummaries.size(), battleSummaries.size(), finalSummaries.size());
            
            return result;
        } catch (Exception e) {
            log.error("전체 요약 조회 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
            return DebateSummaryResponse.builder()
                .roomId(roomId)
                .opinion(new ArrayList<>())
                .battle(new ArrayList<>())
                .final_summary(new ArrayList<>())
                .current_phase("unknown")
                .current_round(0)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }
    
    /**
     * 의견 요약 조회
     */
    public List<OpinionSummary> getOpinionSummaries(Long roomId) {
        try {
            String pattern = SUMMARY_PREFIX + roomId + ":opinion:*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            List<DebateSummaryResponse.OpinionSummary> summaries = new ArrayList<>();
            if (keys != null) {
                for (String key : keys) {
                    String summaryJson = redisTemplate.opsForValue().get(key);
                    if (summaryJson != null) {
                        Map<String, Object> summary = objectMapper.readValue(summaryJson, Map.class);
                        
                        DebateSummaryResponse.OpinionSummary opinionSummary = DebateSummaryResponse.OpinionSummary.builder()
                            .phase((String) summary.get("phase"))
                            .round((Integer) summary.get("round"))
                            .user_id((String) summary.get("user_id"))
                            .text((String) summary.get("text"))
                            .team((String) summary.get("team"))
                            .timestamp((String) summary.get("timestamp"))
                            .build();
                        
                        summaries.add(opinionSummary);
                    }
                }
                
                // 라운드 순으로 정렬
                summaries.sort((a, b) -> a.getRound().compareTo(b.getRound()));
            }
            
            return summaries;
        } catch (Exception e) {
            log.error("의견 요약 조회 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 공방전 요약 조회
     */
    public List<DebateSummaryResponse.BattleSummary> getBattleSummaries(Long roomId) {
        try {
            String pattern = SUMMARY_PREFIX + roomId + ":battle:*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            List<DebateSummaryResponse.BattleSummary> summaries = new ArrayList<>();
            if (keys != null) {
                for (String key : keys) {
                    String summaryJson = redisTemplate.opsForValue().get(key);
                    if (summaryJson != null) {
                        Map<String, Object> summary = objectMapper.readValue(summaryJson, Map.class);
                        
                        DebateSummaryResponse.BattleSummary battleSummary = DebateSummaryResponse.BattleSummary.builder()
                            .phase((String) summary.get("phase"))
                            .round((Integer) summary.get("round"))
                            .attack_id((String) summary.get("attack_id"))
                            .defense_id((String) summary.get("defense_id"))
                            .text((String) summary.get("text"))
                            .rebuttal_score((Integer) summary.get("rebuttal_score"))
                            .attack_team((String) summary.get("attack_team"))
                            .defense_team((String) summary.get("defense_team"))
                            .timestamp((String) summary.get("timestamp"))
                            .build();
                        
                        summaries.add(battleSummary);
                    }
                }
                
                // 라운드 순으로 정렬
                summaries.sort((a, b) -> a.getRound().compareTo(b.getRound()));
            }
            
            return summaries;
        } catch (Exception e) {
            log.error("공방전 요약 조회 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 최종 요약 조회
     */
    public List<DebateSummaryResponse.FinalSummary> getFinalSummaries(Long roomId) {
        try {
            String pattern = SUMMARY_PREFIX + roomId + ":final:*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            List<DebateSummaryResponse.FinalSummary> summaries = new ArrayList<>();
            if (keys != null) {
                for (String key : keys) {
                    String summaryJson = redisTemplate.opsForValue().get(key);
                    if (summaryJson != null) {
                        Map<String, Object> summary = objectMapper.readValue(summaryJson, Map.class);
                        
                        // Votes 객체 생성
                        Map<String, Object> votesMap = (Map<String, Object>) summary.get("votes");
                        DebateSummaryResponse.Votes votes = null;
                        if (votesMap != null) {
                            votes = DebateSummaryResponse.Votes.builder()
                                .num1((Integer) votesMap.get("num1"))
                                .num2((Integer) votesMap.get("num2"))
                                .none((Integer) votesMap.get("none"))
                                .build();
                        }
                        
                        // SoftScores 객체 생성
                        Map<String, Object> softScoresMap = (Map<String, Object>) summary.get("soft_scores");
                        DebateSummaryResponse.SoftScores softScores = null;
                        if (softScoresMap != null) {
                            softScores = DebateSummaryResponse.SoftScores.builder()
                                .num1((Double) softScoresMap.get("num1"))
                                .num2((Double) softScoresMap.get("num2"))
                                .build();
                        }
                        
                        // FullSummarize 객체 생성
                        Map<String, Object> fullSummarizeMap = (Map<String, Object>) summary.get("full_summarize");
                        DebateSummaryResponse.FullSummarize fullSummarize = null;
                        if (fullSummarizeMap != null) {
                            fullSummarize = DebateSummaryResponse.FullSummarize.builder()
                                .num1((String) fullSummarizeMap.get("num1"))
                                .num2((String) fullSummarizeMap.get("num2"))
                                .build();
                        }
                        
                        DebateSummaryResponse.FinalSummary finalSummary = DebateSummaryResponse.FinalSummary.builder()
                            .phase((String) summary.get("phase"))
                            .round((Integer) summary.get("round"))
                            .winner((String) summary.get("winner"))
                            .votes(votes)
                            .soft_scores(softScores)
                            .juror_explain((String) summary.get("juror_explain"))
                            .full_summarize(fullSummarize)
                            .timestamp((String) summary.get("timestamp"))
                            .build();
                        
                        summaries.add(finalSummary);
                    }
                }
            }
            
            return summaries;
        } catch (Exception e) {
            log.error("최종 요약 조회 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 현재 단계 조회
     */
    private String getCurrentPhase(Long roomId) {
        try {
            // 최종 요약이 있으면 final
            if (!getFinalSummaries(roomId).isEmpty()) {
                return "final";
            }
            
            // 공방전 요약이 있으면 battle
            if (!getBattleSummaries(roomId).isEmpty()) {
                return "battle";
            }
            
            // 의견 요약이 있으면 opinion
            if (!getOpinionSummaries(roomId).isEmpty()) {
                return "opinion";
            }
            
            return "waiting";
        } catch (Exception e) {
            log.error("현재 단계 조회 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
            return "unknown";
        }
    }
    
    /**
     * 현재 라운드 조회
     */
    private Integer getCurrentRound(Long roomId) {
        try {
            String currentPhase = getCurrentPhase(roomId);
            
            switch (currentPhase) {
                case "final":
                    return 1;
                case "battle":
                    List<DebateSummaryResponse.BattleSummary> battleSummaries = getBattleSummaries(roomId);
                    return battleSummaries.size();
                case "opinion":
                    List<DebateSummaryResponse.OpinionSummary> opinionSummaries = getOpinionSummaries(roomId);
                    return opinionSummaries.size();
                default:
                    return 0;
            }
        } catch (Exception e) {
            log.error("현재 라운드 조회 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 방의 모든 요약 데이터 삭제
     */
    public void deleteRoomSummaries(Long roomId) {
        try {
            String pattern = SUMMARY_PREFIX + roomId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("방 요약 데이터 삭제 완료 - roomId: {}, 삭제된 키 수: {}", roomId, keys.size());
            }
        } catch (Exception e) {
            log.error("방 요약 데이터 삭제 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
        }
    }
    
    /**
     * TTL 연장
     */
    public void extendTTL(Long roomId, Duration duration) {
        try {
            String pattern = SUMMARY_PREFIX + roomId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                for (String key : keys) {
                    redisTemplate.expire(key, duration);
                }
                log.info("요약 데이터 TTL 연장 완료 - roomId: {}, duration: {}, 키 수: {}", roomId, duration, keys.size());
            }
        } catch (Exception e) {
            log.error("요약 데이터 TTL 연장 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
        }
    }
}
