package com.memes.api.service;

import com.memes.api.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyRateLimiter {

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties rateLimitProperties;

    public boolean isAllowed(Long keyId, String role) {
        Bandwidth limit = resolveLimit(role);
        String redisKey = "rate_limit:" + keyId;
        Bucket bucket = proxyManager.builder()
            .build(redisKey, BucketConfiguration.builder().addLimit(limit).build());
        return bucket.tryConsume(1);
    }

    private Bandwidth resolveLimit(String role) {
        return switch (role) {
            case "ADMIN" -> {
                int limit = rateLimitProperties.getAdminPerMinute();
                yield Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofMinutes(1)));
            }
            case "WRITE" -> {
                int limit = rateLimitProperties.getWritePerMinute();
                yield Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofMinutes(1)));
            }
            default -> {
                int limit = rateLimitProperties.getReadPerMinute();
                yield Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofMinutes(1)));
            }
        };
    }
}
