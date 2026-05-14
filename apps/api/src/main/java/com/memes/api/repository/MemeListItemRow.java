package com.memes.api.repository;

import lombok.Builder;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record MemeListItemRow(
    long id,
    String slug,
    int score,
    @Nullable OffsetDateTime createdAt,
    String categorySlug,
    @Nullable String authorUsername,
    String title,
    @Nullable String description,
    @Nullable String imagePath,
    List<String> tagSlugs
) {}
