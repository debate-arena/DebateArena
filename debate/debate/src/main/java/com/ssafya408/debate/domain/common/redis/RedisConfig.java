package com.ssafya408.debate.domain.common.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Key는 문자열
    template.setKeySerializer(new StringRedisSerializer());

    // Hash Key는 문자열
    template.setHashKeySerializer(new StringRedisSerializer());

    // Hash Value는 Jackson으로 직렬화
    template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

    // 일반 value도 Jackson 사용 가능
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

    template.afterPropertiesSet();
    return template;
  }

}
