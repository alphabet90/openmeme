package com.memes.api.modules.admin;

import com.memes.api.common.dto.GetStatsInput;
import com.memes.api.common.operation.Operation;
import com.memes.api.models.ApiKey;
import com.memes.api.mappers.ApiKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListApiKeysOperation implements Operation<GetStatsInput, List<ApiKey>> {

    private final ApiKeyMapper apiKeyMapper;

    @Override
    public List<ApiKey> execute(GetStatsInput input) {
        return apiKeyMapper.selectAllActive();
    }
}
