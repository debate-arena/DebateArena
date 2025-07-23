package com.ssafya408.debatearena.secuirty.db;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  private String name;

  private String provider; // 예: google, kakao 등

  private LocalDateTime createdAt;

  // 필요한 경우 role, createdDate 등도 추가 가능
}
