package com.ssafya408.debatearena.service.topic.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "topic")
@Getter
@Setter
@NoArgsConstructor
public class Topic {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "topic_id" )
  private Long id;

  private String topicText;

  private String firstOption;
  private String secondOption;

  @CreatedDate
  private LocalDateTime createdAt;

}
