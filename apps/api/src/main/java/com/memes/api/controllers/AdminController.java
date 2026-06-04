package com.memes.api.controllers;

import com.memes.api.common.dto.IndexMemeInput;
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
        boolean memes = Optional.ofNullable(indexMemes).orElse(true);
        boolean categories = Optional.ofNullable(indexCategories).orElse(true);
        triggerIndexOperation.executeAsync(new IndexMemeInput(body, memes, categories));
        ReindexAccepted accepted = new ReindexAccepted();
        accepted.setStatus(ReindexAccepted.StatusEnum.ACCEPTED);
        return ResponseEntity.ok(accepted);
    }

    @Override
    public ResponseEntity<List<ApiKey>> listApiKeys() {
        return ResponseEntity.ok(listApiKeysOperation.execute(null));
    }

    @Override
    public ResponseEntity<ApiKeyCreated> createApiKey(ApiKeyCreateRequest body) {
        CreateApiKeyOperation.Result result = createApiKeyOperation.execute(body);
        return ResponseEntity.status(201).body(createApiKeyOperation.toCreated(result));
    }

    @Override
    public ResponseEntity<Void> revokeApiKey(Long id) {
        revokeApiKeyOperation.execute(id);
        return ResponseEntity.noContent().build();
    }
}
