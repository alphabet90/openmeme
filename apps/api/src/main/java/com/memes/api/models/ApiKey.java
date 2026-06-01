package com.memes.api.models;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ApiKey {
    private Long id;
    private String clientName;
    private String keyHash;
    private String keySalt;
    private String role;
    private Boolean active;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUsedAt;
}
