package com.arena.test04spring.config;

import com.arena.test04spring.subscriber.SocketMessageListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;


@Configuration
@RequiredArgsConstructor
public class RedisConfig {



    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            SocketMessageListener socketMessageListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                socketMessageListener, new ChannelTopic("mediasoup:router:created")
        );
        container.addMessageListener(
                socketMessageListener, new ChannelTopic("mediasoup:transport:created")
        );
        container.addMessageListener(
                socketMessageListener, new ChannelTopic("mediasoup:transport:connected")
        );
        container.addMessageListener(
                socketMessageListener, new ChannelTopic("mediasoup:producer:created")
        );
        container.addMessageListener(
                socketMessageListener, new ChannelTopic("mediasoup:consumer:created")
        );
        return container;
    }
}