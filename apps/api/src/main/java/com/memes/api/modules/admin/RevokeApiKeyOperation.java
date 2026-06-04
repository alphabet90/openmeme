package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.mappers.ApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevokeApiKeyOperation implements Operation<Long, Void> {

    private final ApiKeyMapper apiKeyMapper;

    @Override
    public Void execute(Long id) {
        apiKeyMapper.deactivate(id);
        return null;
    }
}
