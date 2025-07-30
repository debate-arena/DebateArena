package com.ssafya408.debate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DebateApplication {

  public static void main(String[] args) {
    SpringApplication.run(DebateApplication.class, args);
  }

}
