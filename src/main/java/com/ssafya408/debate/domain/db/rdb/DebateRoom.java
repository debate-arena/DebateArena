package com.ssafya408.debate.domain.db.rdb;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "dabate_room")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@Getter
public class DebateRoom {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)

  @Column(name = "room_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "topic_id")
  private Topic topic;
  @Enumerated(EnumType.STRING)
  private MatchType mathType;
  private Integer winTeam;

  @CreatedDate
  private LocalDateTime createdAt;

  private DebateRoom(Topic topic, MatchType mathType) {
    this.topic=topic;
    this.mathType=mathType;
  }

  public static DebateRoom generateDebateRoom(Topic topic, MatchType mathType) {
    return new DebateRoom(topic, mathType);
  }

}
