package com.memes.api.models;

import lombok.Data;

@Data
public class CategoryTranslation {
    private Long id;
    private Long categoryId;
    private String locale;
    private String name;
    private String description;
}
