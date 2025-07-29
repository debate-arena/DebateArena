package com.ssafya408.debatearena.secuirty.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
  Optional<User> findByEmail(String email);
  
  // 닉네임 중복 확인
  boolean existsByNickname(String nickname);
  
  // 닉네임으로 사용자 찾기
  Optional<User> findByNickname(String nickname);
}
