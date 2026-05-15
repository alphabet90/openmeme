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
            log.info("============================================================");
            log.info("EMERGENCY: No active ADMIN API keys found. A bootstrap key");
            log.info("has been created. Copy the key below immediately — it will");
            log.info("never be shown again. Revoke it after creating your own.");
            log.info("BOOTSTRAP API KEY: {}", result.plainKey());
            log.info("============================================================");
        }
    }
}
