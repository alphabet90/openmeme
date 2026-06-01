package com.memes.api.modules.admin;

import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record MemeTranslationRow(
    String locale,
    String title,
    @Nullable String description
) {}
