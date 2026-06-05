package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.models.ApiKey;
import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.generated.model.ApiKey.RoleEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListApiKeysOperation implements Operation<Void, List<com.memes.api.generated.model.ApiKey>> {

    private final ApiKeyMapper apiKeyMapper;

    @Override
    public List<com.memes.api.generated.model.ApiKey> execute(Void input) {
        return apiKeyMapper.selectAllActive().stream()
            .map(this::toGenerated)
            .toList();
    }

    private com.memes.api.generated.model.ApiKey toGenerated(com.memes.api.models.ApiKey entity) {
        com.memes.api.generated.model.ApiKey key = new com.memes.api.generated.model.ApiKey();
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
