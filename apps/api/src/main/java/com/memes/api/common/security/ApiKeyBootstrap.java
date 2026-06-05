package com.memes.api.common.security;

import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.modules.admin.CreateApiKeyOperation;
import com.memes.api.generated.model.ApiKeyCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyBootstrap implements CommandLineRunner {

    private final ApiKeyMapper apiKeyMapper;
    private final CreateApiKeyOperation createApiKeyOperation;

    @Override
    public void run(String... args) {
        int activeAdmins = apiKeyMapper.countActiveAdminKeys();
        if (activeAdmins == 0) {
            ApiKeyCreateRequest req = new ApiKeyCreateRequest();
            req.setClientName("bootstrap-emergency");
            req.setRole(ApiKeyCreateRequest.RoleEnum.ADMIN);
            req.setExpiresAt(null);
            CreateApiKeyOperation.Result result = createApiKeyOperation.execute(req);
            log.info("============================================================");
            log.info("EMERGENCY: No active ADMIN API keys found. A bootstrap key");
            log.info("has been created. Copy the key below immediately — it will");
            log.info("never be shown again. Revoke it after creating your own.");
            log.info("BOOTSTRAP API KEY: {}", result.getPlainKey());
            log.info("============================================================");
        }
    }
}
