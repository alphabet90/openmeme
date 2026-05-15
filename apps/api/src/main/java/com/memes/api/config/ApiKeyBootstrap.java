package com.memes.api.config;

import com.memes.api.repository.ApiKeyRepository;
import com.memes.api.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyBootstrap implements CommandLineRunner {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;

    @Override
    public void run(String... args) {
        int activeAdmins = apiKeyRepository.countActiveAdminKeys();
        if (activeAdmins == 0) {
            ApiKeyService.ApiKeyCreationResult result = apiKeyService.createKey(
                "bootstrap-emergency", "ADMIN", null);
            log.warn("============================================================");
            log.warn("EMERGENCY: No active ADMIN API keys found. A bootstrap key");
            log.warn("has been created. Copy the key below immediately — it will");
            log.warn("never be shown again. Revoke it after creating your own.");
            log.warn("BOOTSTRAP API KEY: {}", result.plainKey());
            log.warn("============================================================");
        }
    }
}
