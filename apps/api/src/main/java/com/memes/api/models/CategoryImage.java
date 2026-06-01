package com.memes.api.models;

import lombok.Data;

@Data
public class CategoryImage {
    private Long id;
    private Long categoryId;
    private String path;
    private Integer width;
    private Integer height;
    private Long bytes;
    private String mimeType;
    private String imageType;
    private Integer position;
    private Boolean isPrimary;
}
