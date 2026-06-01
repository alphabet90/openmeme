package com.memes.api.controllers;

import com.memes.api.common.security.ApiKeyRateLimiter;
import com.memes.api.config.LocaleCodeConverter;
import com.memes.api.config.LoggingProperties;
import com.memes.api.config.SecurityConfig;
import com.memes.api.filter.ApiKeyAuthenticationFilter;
import com.memes.api.filter.RateLimitingFilter;
import com.memes.api.generated.model.ApiKeyCreateRequest;
import com.memes.api.generated.model.ApiKeyCreated;
import com.memes.api.generated.model.ReindexAccepted;
import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.models.ApiKey;
import com.memes.api.modules.admin.CreateApiKeyOperation;
import com.memes.api.modules.admin.ListApiKeysOperation;
import com.memes.api.modules.admin.RevokeApiKeyOperation;
import com.memes.api.modules.admin.TriggerIndexOperation;
import com.memes.api.util.ApiKeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({AdminController.class, SecurityConfig.class,
    ApiKeyAuthenticationFilter.class, RateLimitingFilter.class,
    LoggingProperties.class, LocaleCodeConverter.class})
@TestPropertySource(properties = {
    "spring.cache.type=none",
    "memes.cdn-url=https://cdn.example.com"
})
class AdminControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TriggerIndexOperation triggerIndexOperation;
    @MockBean ListApiKeysOperation listApiKeysOperation;
    @MockBean CreateApiKeyOperation createApiKeyOperation;
    @MockBean RevokeApiKeyOperation revokeApiKeyOperation;
    @MockBean ApiKeyMapper apiKeyMapper;
    @MockBean ApiKeyRateLimiter apiKeyRateLimiter;

    private static final String ADMIN_KEY = "test-admin-key";

    @BeforeEach
    void setUp() {
        ApiKey key = new ApiKey();
        key.setId(1L);
        key.setKeyHash(ApiKeyHasher.hash(ADMIN_KEY));
        key.setClientName("TestAdmin");
        key.setRole("ADMIN");
        key.setActive(true);
        when(apiKeyMapper.findByKeyHash(anyString())).thenReturn(Optional.empty());
        when(apiKeyMapper.findByKeyHash(ApiKeyHasher.hash(ADMIN_KEY))).thenReturn(Optional.of(key));
        when(apiKeyRateLimiter.isAllowed(any(), anyString())).thenReturn(true);
    }

    @Test
    void reindex_withoutKey_returns401() throws Exception {
        mockMvc.perform(post("/admin/reindex"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void reindex_withAdminKey_returns200() throws Exception {
        mockMvc.perform(post("/admin/reindex").header("X-Api-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("accepted"));
    }

    @Test
    void reindex_withReadKey_returns403() throws Exception {
        ApiKey readKey = new ApiKey();
        readKey.setId(2L);
        readKey.setKeyHash(ApiKeyHasher.hash("read-key"));
        readKey.setClientName("ReadOnly");
        readKey.setRole("READ");
        readKey.setActive(true);
        when(apiKeyMapper.findByKeyHash(ApiKeyHasher.hash("read-key"))).thenReturn(Optional.of(readKey));

        mockMvc.perform(post("/admin/reindex").header("X-Api-Key", "read-key"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listApiKeys_returns200() throws Exception {
        com.memes.api.models.ApiKey key = new com.memes.api.models.ApiKey();
        key.setId(1L);
        key.setClientName("test-client");
        key.setRole("READ");
        key.setActive(true);
        when(listApiKeysOperation.execute(null)).thenReturn(List.of(key));

        mockMvc.perform(get("/admin/api-keys").header("X-Api-Key", ADMIN_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].client_name").value("test-client"));
    }

    @Test
    void createApiKey_returns201() throws Exception {
        when(createApiKeyOperation.execute(any())).thenReturn(
            new CreateApiKeyOperation.Result(42L, "sk-abc123"));

        mockMvc.perform(post("/admin/api-keys")
                .header("X-Api-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"client_name":"new-client","role":"READ"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.key").value("sk-abc123"));
    }

    @Test
    void revokeApiKey_returns204() throws Exception {
        mockMvc.perform(delete("/admin/api-keys/5").header("X-Api-Key", ADMIN_KEY))
            .andExpect(status().isNoContent());
    }
}
