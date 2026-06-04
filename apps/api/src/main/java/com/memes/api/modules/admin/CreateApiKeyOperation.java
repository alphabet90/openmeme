package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.models.ApiKey;
import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.util.ApiKeyGenerator;
import com.memes.api.util.ApiKeyHasher;
import com.memes.api.generated.model.ApiKeyCreateRequest;
import com.memes.api.generated.model.ApiKeyCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateApiKeyOperation implements Operation<ApiKeyCreateRequest, CreateApiKeyOperation.Result> {

    private final ApiKeyMapper apiKeyMapper;

    @Override
    public Result execute(ApiKeyCreateRequest input) {
        String plain = ApiKeyGenerator.generate();
        String hash = ApiKeyHasher.hash(plain);
        ApiKey entity = new ApiKey();
        entity.setKeyHash(hash);
        entity.setClientName(input.getClientName());
        entity.setRole(input.getRole().getValue());
        entity.setActive(true);
        entity.setExpiresAt(input.getExpiresAt());
        apiKeyMapper.insert(entity);
        return new Result(entity.getId(), plain);
    }

    public ApiKeyCreated toCreated(Result result) {
        ApiKeyCreated created = new ApiKeyCreated();
        created.setId(result.getId());
        created.setKey(result.getPlainKey());
        return created;
    }

    @Data
    public static class Result {
        private final long id;
        private final String plainKey;

        public Result(long id, String plainKey) {
            this.id = id;
            this.plainKey = plainKey;
        }
    }
}
