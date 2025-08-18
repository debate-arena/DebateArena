package com.ssafya408.debate.domain.api.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageListener implements MessageListener{
    @Override
    public void onMessage(Message message, byte[] pattern) {

    }
}
