package com.ssafya408.matching.common.topic.service;

import com.ssafya408.matching.api.dto.MatchApplyRequest;
import com.ssafya408.matching.api.dto.WaitingUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchInfo {
  private String matchId;
  private Long topicId;
  private Integer topicIdx;
  private Integer type;
  private Integer totalPlayer;

  private List<List<WaitingUser>> teams;

  private Map<WaitingUser, MatchApplyRequest> candidates;
  private Map<String, Boolean> acceptResponse;

  public MatchInfo(Long topicId, Integer topicIdx,Integer type,List<List<WaitingUser>> teams){
    String matchId = UUID.randomUUID().toString();
    this.matchId=matchId;
    this.topicId=topicId;
    this.topicIdx=topicIdx;
    this.type=type;
    this.teams=teams;
    this.totalPlayer= teams.size()*(type+1);

    acceptResponse = new ConcurrentHashMap<>(); // 여러 스레드에서 수정하기에 Tread-Safe 하게 처리

  }

  public static MatchInfo generateMatchInfo(Long topicId, Integer topicIdx,Integer type
      ,List<List<WaitingUser>> teams) {
    return new MatchInfo(topicId,topicIdx,type,teams);

  }

  public
  void saveCandidateInfo( Map<WaitingUser, MatchApplyRequest> candidates) {
    this.candidates=candidates;
  }

  public Map<String,Boolean> updateMatchResponse(String user, Boolean answer) {
    this.acceptResponse.put(user, answer);
    return this.acceptResponse;
  }

}
