package com.memes.api.controller;

import com.memes.api.generated.api.AdminApiDelegate;
import com.memes.api.generated.model.ApiKey;
import com.memes.api.generated.model.ApiKeyCreateRequest;
import com.memes.api.generated.model.ApiKeyCreated;
import com.memes.api.generated.model.MemeIndexRequest;
import com.memes.api.generated.model.ReindexAccepted;
import com.memes.api.repository.ApiKeyRow;
import com.memes.api.service.ApiKeyService;
import com.memes.api.service.IndexerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminApiDelegateImpl implements AdminApiDelegate {

    private final IndexerService indexerService;
    private final ApiKeyService apiKeyService;

    @Override
    public ResponseEntity<ReindexAccepted> reindex(MemeIndexRequest body) {
        indexerService.reindexAsync(body);
        ReindexAccepted accepted = new ReindexAccepted();
        accepted.setStatus(ReindexAccepted.StatusEnum.ACCEPTED);
        return ResponseEntity.ok(accepted);
    }

    @Override
    public ResponseEntity<List<ApiKey>> listApiKeys() {
        List<ApiKey> keys = apiKeyService.listKeys().stream()
            .map(this::toGenerated)
            .toList();
        return ResponseEntity.ok(keys);
    }

    @Override
    public ResponseEntity<ApiKeyCreated> createApiKey(ApiKeyCreateRequest body) {
        ApiKeyService.ApiKeyCreationResult result = apiKeyService.createKey(
            body.getClientName(), body.getRole().getValue(), body.getExpiresAt());
        ApiKeyCreated created = new ApiKeyCreated();
        created.setId(result.id());
        created.setKey(result.plainKey());
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<Void> revokeApiKey(Long id) {
        apiKeyService.revokeKey(id);
        return ResponseEntity.noContent().build();
    }

    private ApiKey toGenerated(ApiKeyRow row) {
        ApiKey key = new ApiKey();
        key.setId(row.id());
        key.setClientName(row.clientName());
        key.setRole(ApiKey.RoleEnum.fromValue(row.role()));
        key.setActive(row.active());
        key.setExpiresAt(row.expiresAt());
        key.setCreatedAt(row.createdAt());
        key.setLastUsedAt(row.lastUsedAt());
        return key;
    }
}
