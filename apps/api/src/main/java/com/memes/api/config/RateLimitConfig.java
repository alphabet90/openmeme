package com.memes.api.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean(destroyMethod = "close")
    public io.lettuce.core.api.StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(
            LettuceConnectionFactory connectionFactory) {
        RedisClient client = (RedisClient) connectionFactory.getNativeClient();
        if (client == null) {
            throw new IllegalStateException("Native Lettuce RedisClient is not available");
        }
        return client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public ProxyManager<String> lettuceProxyManager(
            io.lettuce.core.api.StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection)
            .withExpirationStrategy(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10))
            )
            .build();
    }
}
