package com.memes.api.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.models.ApiKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyMapper apiKeyMapper;

    private final Cache<String, ApiKey> apiKeyCache = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-Api-Key");
        if (apiKey == null || apiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String hash = com.memes.api.util.ApiKeyHasher.hash(apiKey);
        Optional<ApiKey> cached = Optional.ofNullable(apiKeyCache.getIfPresent(hash));
        Optional<ApiKey> result = cached.or(() -> {
            Optional<ApiKey> fetched = apiKeyMapper.selectByKeyHash(hash);
            fetched.ifPresent(k -> apiKeyCache.put(hash, k));
            return fetched;
        });

        result.ifPresentOrElse(row -> {
            if (!Boolean.TRUE.equals(row.getActive())) {
                log.debug("API key rejected: inactive");
                return;
            }
            if (row.getExpiresAt() != null && row.getExpiresAt().isBefore(OffsetDateTime.now())) {
                log.debug("API key rejected: expired");
                return;
            }
            var token = new ApiKeyAuthenticationToken(
                row.getId(), row.getRole(), row.getClientName(), apiKey);
            SecurityContextHolder.getContext().setAuthentication(token);
            CompletableFuture.runAsync(() -> {
                try { apiKeyMapper.updateLastUsed(row.getId()); }
                catch (Exception e) { log.warn("Failed to update last_used_at for keyId={}", row.getId(), e); }
            });
        }, () -> log.debug("API key rejected: unknown hash"));

        filterChain.doFilter(request, response);
    }
}
