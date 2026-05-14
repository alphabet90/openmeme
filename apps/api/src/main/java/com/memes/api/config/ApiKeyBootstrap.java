package com.memes.api.config;

import com.memes.api.repository.ApiKeyRepository;
import com.memes.api.service.ApiKeyService;
import com.memes.api.util.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyBootstrap implements ApplicationRunner {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyService apiKeyService;

    @Value("${memes.admin-api-key:}")
    private String bootstrapKey;

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapKey == null || bootstrapKey.isBlank()) {
            return;
        }
        int adminCount = apiKeyRepository.countActiveAdminKeys();
        if (adminCount == 0) {
            log.info("No active ADMIN keys found. Migrating bootstrap key from environment into database.");
            String hash = ApiKeyHasher.hash(bootstrapKey);
            apiKeyRepository.findByKeyHash(hash).ifPresentOrElse(
                key -> log.info("Bootstrap key already exists in database."),
                () -> {
                    apiKeyService.createKeyFromPlaintext("Bootstrap Admin", "ADMIN", null, bootstrapKey);
                    log.warn("Bootstrap ADMIN API key has been migrated to the database. "
                        + "It is recommended to rotate this key via POST /admin/api-keys and remove ADMIN_API_KEY from environment.");
                }
            );
        }
    }
}
