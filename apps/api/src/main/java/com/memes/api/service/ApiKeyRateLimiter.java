package com.memes.api.service;

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

    public boolean isAllowed(Long keyId, String role) {
        Bandwidth limit = resolveLimit(role);
        String redisKey = "rate_limit:" + keyId;
        Bucket bucket = proxyManager.builder()
            .build(redisKey, BucketConfiguration.builder().addLimit(limit).build());
        return bucket.tryConsume(1);
    }

    private Bandwidth resolveLimit(String role) {
        return switch (role) {
            case "ADMIN" -> Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));
            case "WRITE" -> Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)));
            default -> Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        };
    }
}
