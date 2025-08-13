package com.ssafya408.matching.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateRoomResponse {
  private Long roomId;
  private List<DebateMemberDto> firstTeam;
  private List<DebateMemberDto> secondTeam;
}
