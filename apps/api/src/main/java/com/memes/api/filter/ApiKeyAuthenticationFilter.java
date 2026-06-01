package com.memes.api.filter;

import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.models.ApiKey;
import com.memes.api.security.ApiKeyAuthenticationToken;
import com.memes.api.util.ApiKeyHasher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Api-Key";

    private final ApiKeyMapper apiKeyMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedKey = request.getHeader(HEADER);

        if (providedKey == null || providedKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String hash = ApiKeyHasher.hash(providedKey);
        Optional<ApiKey> keyOpt = apiKeyMapper.findByKeyHash(hash);

        if (keyOpt.isEmpty() || !isValid(keyOpt.get())) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        ApiKey key = keyOpt.get();
        long keyId = Optional.ofNullable(key.getId()).orElse(0L);
        ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(
            keyId, key.getRole(), key.getClientName(), providedKey);
        SecurityContextHolder.getContext().setAuthentication(auth);

        CompletableFuture.runAsync(() -> {
            try {
                apiKeyMapper.updateLastUsed(keyId);
            } catch (Exception ex) {
                log.warn("Failed to update last_used_at for keyId={}", keyId, ex);
            }
        });

        filterChain.doFilter(request, response);
    }

    private boolean isValid(ApiKey key) {
        if (!Boolean.TRUE.equals(key.getActive())) {
            return false;
        }
        if (key.getExpiresAt() == null) {
            return true;
        }
        return key.getExpiresAt().isAfter(OffsetDateTime.now());
    }
}
