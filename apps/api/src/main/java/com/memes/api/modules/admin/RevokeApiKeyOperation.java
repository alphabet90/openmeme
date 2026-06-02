package com.memes.api.modules.admin;

import com.memes.api.common.dto.RevokeApiKeyInput;
import com.memes.api.common.operation.Operation;
import com.memes.api.mappers.ApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevokeApiKeyOperation implements Operation<RevokeApiKeyInput, Void> {

    private final ApiKeyMapper apiKeyMapper;

    @Override
    public Void execute(RevokeApiKeyInput input) {
        apiKeyMapper.deactivate(input.getId());
        return null;
    }
}
