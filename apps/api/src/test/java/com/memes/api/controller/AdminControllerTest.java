package com.memes.api.controller;

import com.memes.api.config.LocaleCodeConverter;
import com.memes.api.config.LoggingProperties;
import com.memes.api.config.SecurityConfig;
import com.memes.api.filter.ApiKeyAuthenticationFilter;
import com.memes.api.filter.RateLimitingFilter;
import com.memes.api.repository.ApiKeyRepository;
import com.memes.api.repository.ApiKeyRow;
import com.memes.api.service.ApiKeyRateLimiter;
import com.memes.api.service.ApiKeyService;
import com.memes.api.service.IndexerService;
import com.memes.api.service.MemeService;
import com.memes.api.util.ApiKeyHasher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({MemesApiDelegateImpl.class, AdminApiDelegateImpl.class, SecurityConfig.class,
    ApiKeyAuthenticationFilter.class, RateLimitingFilter.class,
    LoggingProperties.class, LocaleCodeConverter.class})
@TestPropertySource(properties = {
    "spring.cache.type=none"
})
class AdminControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean IndexerService indexerService;
    @MockBean MemeService memeService;
    @MockBean ApiKeyRepository apiKeyRepository;
    @MockBean ApiKeyRateLimiter apiKeyRateLimiter;
    @MockBean ApiKeyService apiKeyService;

    private static final String ADMIN_KEY = "test-admin-key";
    private static final String READ_KEY = "test-read-key";

    private void givenAdminKey() {
        ApiKeyRow row = ApiKeyRow.builder()
            .id(1L)
            .keyHash(ApiKeyHasher.hash(ADMIN_KEY))
            .clientName("Admin")
            .role("ADMIN")
            .active(true)
            .expiresAt(null)
            .createdAt(OffsetDateTime.now())
            .lastUsedAt(null)
            .build();
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());
        when(apiKeyRepository.findByKeyHash(ApiKeyHasher.hash(ADMIN_KEY))).thenReturn(Optional.of(row));
        when(apiKeyRateLimiter.isAllowed(any(), anyString())).thenReturn(true);
    }

    private void givenReadKey() {
        ApiKeyRow row = ApiKeyRow.builder()
            .id(2L)
            .keyHash(ApiKeyHasher.hash(READ_KEY))
            .clientName("Read")
            .role("READ")
            .active(true)
            .expiresAt(null)
            .createdAt(OffsetDateTime.now())
            .lastUsedAt(null)
            .build();
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());
        when(apiKeyRepository.findByKeyHash(ApiKeyHasher.hash(READ_KEY))).thenReturn(Optional.of(row));
        when(apiKeyRateLimiter.isAllowed(any(), anyString())).thenReturn(true);
    }

    @Test
    void reindex_withoutKey_returns401() throws Exception {
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(post("/admin/reindex"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void reindex_withWrongKey_returns401() throws Exception {
        when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(post("/admin/reindex")
                .header("X-Api-Key", "wrong-key"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void reindex_withReadKey_returns403() throws Exception {
        givenReadKey();
        mockMvc.perform(post("/admin/reindex")
                .header("X-Api-Key", READ_KEY))
            .andExpect(status().isForbidden());
    }

    @Test
    void reindex_withAdminKey_returns200WithAccepted() throws Exception {
        givenAdminKey();
        doNothing().when(indexerService).reindexAsync(any());

        mockMvc.perform(post("/admin/reindex")
                .header("X-Api-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("accepted"));
    }

    @Test
    void createApiKey_withReadKey_returns403() throws Exception {
        givenReadKey();
        mockMvc.perform(post("/admin/api-keys")
                .header("X-Api-Key", READ_KEY)
                .contentType("application/json")
                .content("{\"client_name\":\"Test\",\"role\":\"READ\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void createApiKey_rateLimited_returns429() throws Exception {
        givenAdminKey();
        when(apiKeyRateLimiter.isAllowed(any(), anyString())).thenReturn(false);
        mockMvc.perform(post("/admin/api-keys")
                .header("X-Api-Key", ADMIN_KEY)
                .contentType("application/json")
                .content("{\"client_name\":\"Test\",\"role\":\"READ\"}"))
            .andExpect(status().isTooManyRequests());
    }
}
