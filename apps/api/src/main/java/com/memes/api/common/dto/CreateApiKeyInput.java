package com.memes.api.common.dto;

import java.time.OffsetDateTime;

public record CreateApiKeyInput(String clientName, String role, OffsetDateTime expiresAt) {}
