package com.memes.api.repository;

import lombok.Builder;
import java.time.OffsetDateTime;

@Builder
public record ApiKeyInsert(
    String keyHash,
    String clientName,
    String role,
    boolean active,
    OffsetDateTime expiresAt
) {
}
