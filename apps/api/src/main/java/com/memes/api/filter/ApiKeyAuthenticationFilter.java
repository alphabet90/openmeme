package com.memes.api.filter;

import com.memes.api.repository.ApiKeyRepository;
import com.memes.api.repository.ApiKeyRow;
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

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String providedKey = request.getHeader(HEADER);

        if (providedKey == null || providedKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<ApiKeyRow> keyOpt = apiKeyRepository.findByKeyHash(ApiKeyHasher.hash(providedKey));

        if (keyOpt.isEmpty() || !isValid(keyOpt.get())) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        ApiKeyRow key = keyOpt.get();
        ApiKeyAuthenticationToken auth = new ApiKeyAuthenticationToken(
            key.id(), key.role(), key.clientName(), providedKey);
        SecurityContextHolder.getContext().setAuthentication(auth);

        CompletableFuture.runAsync(() -> {
            try {
                apiKeyRepository.updateLastUsed(key.id());
            } catch (Exception ex) {
                log.warn("Failed to update last_used_at for keyId={}", key.id(), ex);
            }
        });

        filterChain.doFilter(request, response);
    }

    private boolean isValid(ApiKeyRow key) {
        if (!key.active()) {
            return false;
        }
        if (key.expiresAt() == null) {
            return true;
        }
        return key.expiresAt().isAfter(OffsetDateTime.now());
    }
}
