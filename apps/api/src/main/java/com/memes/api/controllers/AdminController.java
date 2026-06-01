package com.memes.api.controllers;

import com.memes.api.common.dto.CreateApiKeyInputDto;
import com.memes.api.common.dto.IndexMemeInput;
import com.memes.api.common.dto.ListApiKeysInput;
import com.memes.api.common.dto.RevokeApiKeyInput;
import com.memes.api.generated.api.AdminApiDelegate;
import com.memes.api.generated.model.ApiKey;
import com.memes.api.generated.model.ApiKeyCreateRequest;
import com.memes.api.generated.model.ApiKeyCreated;
import com.memes.api.generated.model.MemeIndexRequest;
import com.memes.api.generated.model.ReindexAccepted;
import com.memes.api.modules.admin.CreateApiKeyOperation;
import com.memes.api.modules.admin.ListApiKeysOperation;
import com.memes.api.modules.admin.RevokeApiKeyOperation;
import com.memes.api.modules.admin.TriggerIndexOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminController implements AdminApiDelegate {

    private final TriggerIndexOperation triggerIndexOperation;
    private final ListApiKeysOperation listApiKeysOperation;
    private final CreateApiKeyOperation createApiKeyOperation;
    private final RevokeApiKeyOperation revokeApiKeyOperation;

    @Override
    public ResponseEntity<ReindexAccepted> reindex(
        Boolean indexMemes,
        Boolean indexCategories,
        MemeIndexRequest body) {
        boolean memes = indexMemes != null ? indexMemes : true;
        boolean categories = indexCategories != null ? indexCategories : true;
        triggerIndexOperation.executeAsync(new IndexMemeInput(body, memes, categories));
        ReindexAccepted accepted = new ReindexAccepted();
        accepted.setStatus(ReindexAccepted.StatusEnum.ACCEPTED);
        return ResponseEntity.ok(accepted);
    }

    @Override
    public ResponseEntity<List<ApiKey>> listApiKeys() {
        List<ApiKey> keys = listApiKeysOperation.execute(ListApiKeysInput.INSTANCE).stream()
            .map(this::toGenerated)
            .toList();
        return ResponseEntity.ok(keys);
    }

    @Override
    public ResponseEntity<ApiKeyCreated> createApiKey(ApiKeyCreateRequest body) {
        CreateApiKeyOperation.Result result = createApiKeyOperation.execute(
            new CreateApiKeyInputDto(
                body.getClientName(), body.getRole().getValue(), body.getExpiresAt()));
        ApiKeyCreated created = new ApiKeyCreated();
        created.setId(result.id());
        created.setKey(result.plainKey());
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<Void> revokeApiKey(Long id) {
        revokeApiKeyOperation.execute(new RevokeApiKeyInput(id));
        return ResponseEntity.noContent().build();
    }

    private ApiKey toGenerated(com.memes.api.models.ApiKey entity) {
        ApiKey key = new ApiKey();
        key.setId(entity.getId());
        key.setClientName(entity.getClientName());
        key.setRole(ApiKey.RoleEnum.fromValue(entity.getRole()));
        key.setActive(entity.getActive());
        key.setExpiresAt(entity.getExpiresAt());
        key.setCreatedAt(entity.getCreatedAt());
        key.setLastUsedAt(entity.getLastUsedAt());
        return key;
    }
}
