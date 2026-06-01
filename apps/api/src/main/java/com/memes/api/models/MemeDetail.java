package com.memes.api.models;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class MemeDetail {
    long id;
    String slug;
    String categorySlug;
    String defaultLocale;
    String subredditName;
    String authorUsername;
    int score;
    OffsetDateTime createdAt;
    OffsetDateTime indexedAt;
    String sourceUrl;
    String postUrl;
    List<MemeTranslation> translations;
    List<MemeImage> images;
    List<String> tagSlugs;
}
