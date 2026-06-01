package com.memes.api.models;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class MemeListItem {
    long id;
    String slug;
    int score;
    OffsetDateTime createdAt;
    String categorySlug;
    String authorUsername;
    String title;
    String description;
    String imagePath;
    List<String> tagSlugs;
}
