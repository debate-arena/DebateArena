package com.ssafya408.debate.domain.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface TopicRepository extends JpaRepository<Topic,Long> {

}
