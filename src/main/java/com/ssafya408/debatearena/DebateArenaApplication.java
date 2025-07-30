package com.ssafya408.debatearena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@ConfigurationPropertiesScan("com.ssafya408.debatearena.common.security")
public class DebateArenaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DebateArenaApplication.class, args);
	}

}
