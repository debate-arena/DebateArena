package com.ssafya408.matching.domain.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequestMessage {
  private List<ChoiceDto> choiceDtos;
}
