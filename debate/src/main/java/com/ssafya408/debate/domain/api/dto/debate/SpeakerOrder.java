package com.ssafya408.debate.domain.api.dto.debate;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.sql.In;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerOrder implements Serializable {
  private Integer order;
  private String user;
}
