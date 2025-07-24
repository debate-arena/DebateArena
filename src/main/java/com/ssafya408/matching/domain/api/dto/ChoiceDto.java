package com.ssafya408.matching.domain.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChoiceDto {
  private Integer matchType;
  private Integer matchTitle;
  private Integer choice;
}
