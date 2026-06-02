package com.memes.api.common.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class CreateApiKeyInputDto {
    private final String clientName;
    private final String role;
    private final OffsetDateTime expiresAt;
}
