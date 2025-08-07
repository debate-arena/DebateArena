package com.arena.signaling.config;

import com.arena.signaling.listener.RedisMessageListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;


@Configuration
@RequiredArgsConstructor
public class RedisMessageListenerConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisMessageListener redisMessageListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                redisMessageListener, new ChannelTopic("mediasoup:router:created")
        );
        container.addMessageListener(
                redisMessageListener, new ChannelTopic("mediasoup:transport:created")
        );
        container.addMessageListener(
                redisMessageListener, new ChannelTopic("mediasoup:transport:connected")
        );
        container.addMessageListener(
                redisMessageListener, new ChannelTopic("mediasoup:producer:created")
        );
        container.addMessageListener(
                redisMessageListener, new ChannelTopic("mediasoup:consumer:created")
        );
        container.addMessageListener(
                redisMessageListener, new ChannelTopic("mediasoup:transport:established")
        );
        container.addMessageListener(
                redisMessageListener, new ChannelTopic("mediasoup:transport:disconnected")
        );
        container.addMessageListener(
                redisMessageListener, new ChannelTopic("signaling:mic:on")
        );
        container.addMessageListener(
                redisMessageListener, new ChannelTopic("signaling:mic:on")
        );
        return container;
    }
}