package com.ssafya408.matching.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChoiceDto {
    private Integer matchType;
    private Integer matchTitle;
    private Integer choice;
}
