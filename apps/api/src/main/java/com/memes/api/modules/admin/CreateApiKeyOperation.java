package com.memes.api.modules.admin;

import com.memes.api.common.dto.CreateApiKeyInputDto;
import com.memes.api.common.operation.Operation;
import com.memes.api.models.ApiKey;
import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.util.ApiKeyGenerator;
import com.memes.api.util.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateApiKeyOperation implements Operation<CreateApiKeyInputDto, CreateApiKeyOperation.Result> {

    private final ApiKeyMapper apiKeyMapper;

    @Override
    public Result execute(CreateApiKeyInputDto input) {
        String plain = ApiKeyGenerator.generate();
        String hash = ApiKeyHasher.hash(plain);
        ApiKey entity = new ApiKey();
        entity.setKeyHash(hash);
        entity.setClientName(input.clientName());
        entity.setRole(input.role());
        entity.setActive(true);
        entity.setExpiresAt(input.expiresAt());
        apiKeyMapper.insert(entity);
        return new Result(entity.getId(), plain);
    }

    public record Result(long id, String plainKey) {}
}
