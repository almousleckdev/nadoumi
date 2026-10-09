package com.nadoumi.communication.stream;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * The Redis pieces the SSE fan-out needs on top of the connection factory the rest of the app already uses. A
 * dedicated {@link StringRedisTemplate} keeps the plain-JSON payloads independent of the existing
 * {@code RedisTemplate<Object,Object>}'s FastJson2 serializer.
 */
@Configuration
public class RedisStreamConfig {

    @Bean
    public StringRedisTemplate communicationStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisMessageListenerContainer communicationStreamListenerContainer(
            RedisConnectionFactory connectionFactory, StreamMessageListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, List.of(new ChannelTopic(StreamChannels.CONVERSATION)));
        return container;
    }
}
