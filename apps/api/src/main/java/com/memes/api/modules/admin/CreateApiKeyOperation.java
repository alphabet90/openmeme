package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.ApiKeyCreateRequest;
import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.util.ApiKeyGenerator;
import com.memes.api.util.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CreateApiKeyOperation implements Operation<ApiKeyCreateRequest, CreateApiKeyOperation.Result> {

    private final ApiKeyMapper apiKeyMapper;

    @Value
    public static class Result {
        long id;
        String plainKey;
    }

    @Override
    public Result execute(ApiKeyCreateRequest input) {
        var gen = ApiKeyGenerator.generateApiKey();
        String salt = ApiKeyHasher.generateSalt();
        String hashed = ApiKeyHasher.hash(gen.hashedKey(), salt);

        com.memes.api.models.ApiKey key = new com.memes.api.models.ApiKey();
        key.setClientName(input.getClientName());
        key.setKeyHash(hashed);
        key.setKeySalt(salt);
        key.setRole(input.getRole().getValue());
        key.setActive(true);
        key.setExpiresAt(input.getExpiresAt());

        apiKeyMapper.insert(key);
        long id = Optional.ofNullable(key.getId())
            .orElseThrow(() -> new IllegalStateException("API key insert returned null id"));

        return new Result(id, gen.plainKey());
    }
}
