package com.memes.api.models;

import lombok.Value;

@Value
public class SearchHit {
    long memeId;
    String slug;
    String category;
    String title;
    String description;
    int score;
    float rank;
    long totalCount;
}
