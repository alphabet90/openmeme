package com.memes.api.common.dto;

import java.time.OffsetDateTime;

public record CreateApiKeyInputDto(String clientName, String role, OffsetDateTime expiresAt) {}
