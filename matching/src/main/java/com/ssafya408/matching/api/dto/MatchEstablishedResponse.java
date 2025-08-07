package com.ssafya408.matching.api.dto;

import lombok.Builder;

@Builder
public class MatchEstablishedResponse {
  private boolean accept;
  private Integer team;

}
