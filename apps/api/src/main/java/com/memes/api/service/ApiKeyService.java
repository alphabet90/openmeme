package com.memes.api.service;

import com.memes.api.repository.ApiKeyInsert;
import com.memes.api.repository.ApiKeyRepository;
import com.memes.api.repository.ApiKeyRow;
import com.memes.api.util.ApiKeyGenerator;
import com.memes.api.util.ApiKeyHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository repository;

    public ApiKeyCreationResult createKey(String clientName, String role, OffsetDateTime expiresAt) {
        String plain = ApiKeyGenerator.generate();
        long id = insertHash(clientName, role, expiresAt, ApiKeyHasher.hash(plain));
        return new ApiKeyCreationResult(id, plain);
    }

    public record ApiKeyCreationResult(long id, String plainKey) {
    }

    private long insertHash(String clientName, String role, OffsetDateTime expiresAt, String hash) {
        return repository.insert(ApiKeyInsert.builder()
            .keyHash(hash)
            .clientName(clientName)
            .role(role)
            .active(true)
            .expiresAt(expiresAt)
            .build());
    }

    public List<ApiKeyRow> listKeys() {
        return repository.findAllActive();
    }

    public void revokeKey(Long id) {
        repository.deactivate(id);
    }
}
