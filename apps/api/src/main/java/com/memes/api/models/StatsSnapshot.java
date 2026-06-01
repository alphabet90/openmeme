package com.memes.api.models;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class StatsSnapshot {
    private Long totalMemes;
    private Long totalCategories;
    private Long totalSubreddits;
    private String topCategory;
    private OffsetDateTime indexedAt;
}
