package com.memes.api.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.memes.api.mappers.ApiKeyMapper;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private record CachedKeyIdentity(Long id, String role, String clientName) {}

    private final ApiKeyMapper apiKeyMapper;

    private final Cache<String, CachedKeyIdentity> apiKeyCache = Caffeine.newBuilder()
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
        Optional<CachedKeyIdentity> cached = Optional.ofNullable(apiKeyCache.getIfPresent(hash));

        Optional<CachedKeyIdentity> identity = cached.or(() ->
            apiKeyMapper.selectByKeyHash(hash).map(k -> {
                var id = new CachedKeyIdentity(k.getId(), k.getRole(), k.getClientName());
                apiKeyCache.put(hash, id);
                return id;
            })
        );

        identity.ifPresentOrElse(id -> {
            if (!apiKeyMapper.existsActiveById(id.id())) {
                apiKeyCache.invalidate(hash);
                log.debug("API key rejected: inactive or expired");
                return;
            }
            var token = new ApiKeyAuthenticationToken(id.id(), id.role(), id.clientName(), apiKey);
            SecurityContextHolder.getContext().setAuthentication(token);
            CompletableFuture.runAsync(() -> {
                try { apiKeyMapper.updateLastUsed(id.id()); }
                catch (Exception e) { log.warn("Failed to update last_used_at for keyId={}", id.id(), e); }
            });
        }, () -> log.debug("API key rejected: unknown hash"));

        filterChain.doFilter(request, response);
    }
}
