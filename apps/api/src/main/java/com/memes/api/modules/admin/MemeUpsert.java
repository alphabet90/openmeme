package com.memes.api.modules.admin;

import lombok.Builder;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record MemeUpsert(
    String slug,
    String categorySlug,
    String defaultLocale,
    @Nullable String subredditName,
    @Nullable String authorUsername,
    int score,
    @Nullable OffsetDateTime createdAt,
    @Nullable String sourceUrl,
    @Nullable String postUrl,
    List<MemeTranslationRow> translations,
    List<MemeImageRow> images,
    List<String> tagSlugs
) {}
