package com.ssafya408.matching.api.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MatchStatusDto {
  private Integer matchType; //매치 타입, 1대1:[0], 2대2[1]
  private Integer matchTitle; //매치 주제, 1번[0], 2번[1], 상관없음[2]
  private Integer firstQueueSize; //1번 큐 사이즈
  private Integer secondQueueSize; //2번 큐 사이즈
  private Integer thirdQueueSize; //3번 큐 사이즈 (상관 없음)
}
