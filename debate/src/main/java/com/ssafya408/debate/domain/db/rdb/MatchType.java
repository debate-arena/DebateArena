package com.ssafya408.debate.domain.db.rdb;

public enum MatchType {
  ONE_ON_ONE,
  TWO_ON_TWO;

  public static int toInteger(MatchType type) {
    return switch (type){
      case MatchType.ONE_ON_ONE -> 0;
      case MatchType.TWO_ON_TWO -> 1;
      default -> 0;
    };
  }
}
