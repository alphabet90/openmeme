package com.memes.api.controller;

import com.memes.api.common.security.ApiKeyRateLimiter;
import com.memes.api.config.LocaleCodeConverter;
import com.memes.api.config.LoggingProperties;
import com.memes.api.config.SecurityConfig;
import com.memes.api.common.security.ApiKeyAuthenticationFilter;
import com.memes.api.filter.RateLimitingFilter;
import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.models.ApiKey;
import com.memes.api.modules.admin.TriggerIndexOperation;
import com.memes.api.modules.admin.ListApiKeysOperation;
import com.memes.api.modules.admin.CreateApiKeyOperation;
import com.memes.api.modules.admin.RevokeApiKeyOperation;
import com.memes.api.controllers.AdminController;
import com.memes.api.util.ApiKeyHasher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({com.memes.api.controllers.AdminController.class, com.memes.api.controllers.MemesController.class, SecurityConfig.class,
    ApiKeyAuthenticationFilter.class, RateLimitingFilter.class,
    LoggingProperties.class, LocaleCodeConverter.class})
@TestPropertySource(properties = {
    "spring.cache.type=none"
})
class AdminControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TriggerIndexOperation triggerIndexOperation;
    @MockBean ListApiKeysOperation listApiKeysOperation;
    @MockBean CreateApiKeyOperation createApiKeyOperation;
    @MockBean RevokeApiKeyOperation revokeApiKeyOperation;
    @MockBean com.memes.api.modules.memes.GetStatsOperation getStatsOperation;
    @MockBean com.memes.api.modules.memes.ListCategoriesOperation listCategoriesOperation;
    @MockBean com.memes.api.modules.memes.ListMemesOperation listMemesOperation;
    @MockBean com.memes.api.modules.memes.GetMemeOperation getMemeOperation;
    @MockBean com.memes.api.modules.memes.SearchMemesOperation searchMemesOperation;
    @MockBean ApiKeyMapper apiKeyMapper;
    @MockBean ApiKeyRateLimiter apiKeyRateLimiter;

    private static final String ADMIN_KEY = "test-admin-key";
    private static final String READ_KEY = "test-read-key";

    private void givenAdminKey() {
        ApiKey key = new ApiKey();
        key.setId(1L);
        key.setKeyHash(ApiKeyHasher.hash(ADMIN_KEY));
        key.setClientName("Admin");
        key.setRole("ADMIN");
        key.setActive(true);
        when(apiKeyMapper.selectByKeyHash(anyString())).thenReturn(Optional.empty());
        when(apiKeyMapper.selectByKeyHash(ApiKeyHasher.hash(ADMIN_KEY))).thenReturn(Optional.of(key));
        when(apiKeyMapper.existsActiveById(1L)).thenReturn(true);
        when(apiKeyRateLimiter.isAllowed(any(), anyString())).thenReturn(true);
    }

    private void givenReadKey() {
        ApiKey key = new ApiKey();
        key.setId(2L);
        key.setKeyHash(ApiKeyHasher.hash(READ_KEY));
        key.setClientName("Read");
        key.setRole("READ");
        key.setActive(true);
        when(apiKeyMapper.selectByKeyHash(anyString())).thenReturn(Optional.empty());
        when(apiKeyMapper.selectByKeyHash(ApiKeyHasher.hash(READ_KEY))).thenReturn(Optional.of(key));
        when(apiKeyMapper.existsActiveById(2L)).thenReturn(true);
        when(apiKeyRateLimiter.isAllowed(any(), anyString())).thenReturn(true);
    }

    @Test
    void reindex_withoutKey_returns401() throws Exception {
        when(apiKeyMapper.selectByKeyHash(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(post("/admin/reindex"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void reindex_withWrongKey_returns401() throws Exception {
        when(apiKeyMapper.selectByKeyHash(anyString())).thenReturn(Optional.empty());
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
