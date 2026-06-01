package com.memes.api.models;

import lombok.Data;

@Data
public class MemeTranslation {
    private Long id;
    private Long memeId;
    private String locale;
    private String title;
    private String description;
}
