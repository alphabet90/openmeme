package com.memes.api.models;

import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value
@Builder
public class CategoryDetail {
    long id;
    String slug;
    int count;
    int topScore;
    List<CategoryTranslation> translations;
    List<CategoryImage> images;
}
