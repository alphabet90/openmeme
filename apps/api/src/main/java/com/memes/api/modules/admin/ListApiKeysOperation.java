package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.models.ApiKey;
import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.generated.model.ApiKey.RoleEnum;
import com.memes.api.generated.model.ApiKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListApiKeysOperation implements Operation<Void, List<ApiKey>> {

    private final ApiKeyMapper apiKeyMapper;

    @Override
    public List<ApiKey> execute(Void input) {
        return apiKeyMapper.selectAllActive().stream()
            .map(this::toGenerated)
            .toList();
    }

    private ApiKey toGenerated(com.memes.api.models.ApiKey entity) {
        ApiKey key = new ApiKey();
        key.setId(entity.getId());
        key.setClientName(entity.getClientName());
        key.setRole(RoleEnum.fromValue(entity.getRole()));
        key.setActive(entity.getActive());
        key.setExpiresAt(entity.getExpiresAt());
        key.setCreatedAt(entity.getCreatedAt());
        key.setLastUsedAt(entity.getLastUsedAt());
        return key;
    }
}
