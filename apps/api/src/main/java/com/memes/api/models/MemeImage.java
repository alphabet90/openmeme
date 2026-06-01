package com.memes.api.models;

import lombok.Data;

@Data
public class MemeImage {
    private Long id;
    private Long memeId;
    private String path;
    private Integer width;
    private Integer height;
    private Long bytes;
    private String mimeType;
    private Integer position;
    private Boolean isPrimary;
}
