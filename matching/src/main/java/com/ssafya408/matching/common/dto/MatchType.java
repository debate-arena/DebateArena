package com.ssafya408.matching.common.dto;

public enum MatchType {
  ONE_ON_ONE,
  TWO_ON_TWO;

  public static MatchType valueOf(Long id) {
    MatchType type=ONE_ON_ONE;
    if(id==2){
      type=TWO_ON_TWO;
    }

    return type;
  }
}
