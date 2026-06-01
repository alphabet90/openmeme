package com.memes.api.controllers;

import com.memes.api.common.dto.IndexerInput;
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
import java.util.Optional;

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
        triggerIndexOperation.execute(new IndexerInput(body, memes, categories));
        ReindexAccepted accepted = new ReindexAccepted();
        accepted.setStatus(ReindexAccepted.StatusEnum.ACCEPTED);
        return ResponseEntity.ok(accepted);
    }

    @Override
    public ResponseEntity<List<ApiKey>> listApiKeys() {
        List<com.memes.api.models.ApiKey> keys = listApiKeysOperation.execute(null);
        List<ApiKey> result = keys.stream().map(this::toGenerated).toList();
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<ApiKeyCreated> createApiKey(ApiKeyCreateRequest body) {
        CreateApiKeyOperation.Result result = createApiKeyOperation.execute(body);
        ApiKeyCreated created = new ApiKeyCreated();
        created.setId(result.getId());
        created.setKey(result.getPlainKey());
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<Void> revokeApiKey(Long id) {
        revokeApiKeyOperation.execute(id);
        return ResponseEntity.noContent().build();
    }

    private ApiKey toGenerated(com.memes.api.models.ApiKey row) {
        ApiKey key = new ApiKey();
        key.setId(row.getId());
        key.setClientName(row.getClientName());
        key.setRole(ApiKey.RoleEnum.fromValue(row.getRole()));
        key.setActive(row.getActive());
        key.setExpiresAt(row.getExpiresAt());
        key.setCreatedAt(row.getCreatedAt());
        key.setLastUsedAt(row.getLastUsedAt());
        return key;
    }
}
