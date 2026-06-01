package com.memes.api.models;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class Meme {
    private Long id;
    private Long categoryId;
    private String slug;
    private Long subredditId;
    private Long authorId;
    private String defaultLocale;
    private Integer score;
    private String sourceUrl;
    private String postUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime indexedAt;
    private OffsetDateTime deletedAt;
}
