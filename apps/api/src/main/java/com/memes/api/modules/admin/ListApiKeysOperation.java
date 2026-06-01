package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.ApiKey;
import com.memes.api.mappers.ApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListApiKeysOperation implements Operation<Void, List<com.memes.api.models.ApiKey>> {

    private final ApiKeyMapper apiKeyMapper;

    @Override
    public List<com.memes.api.models.ApiKey> execute(Void input) {
        return apiKeyMapper.findAllActive();
    }
}
