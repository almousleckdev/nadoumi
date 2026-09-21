package com.nadoumi.communication.stream;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * The one new piece D6's Redis fan-out plan needs on top of the existing, already
 * in-use {@code RedisConnectionFactory} ({@code ruoyi-framework}'s {@code RedisConfig}
 * backs sessions and the rate limiter off the same factory). A dedicated
 * {@link StringRedisTemplate} keeps this module's plain-JSON pub/sub payloads
 * independent of the existing {@code RedisTemplate<Object,Object>}'s FastJson2
 * value serializer -- same connection factory, same Redis server, a different
 * (simpler) view over it for this one purpose. Not a second Redis client config.
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
        container.addMessageListener(listener, List.of(
                new ChannelTopic(StreamChannels.NOTIFICATION),
                new ChannelTopic(StreamChannels.CONVERSATION)));
        return container;
    }
}
