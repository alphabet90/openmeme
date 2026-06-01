package com.memes.api.modules.admin;

public record CategoryImageRow(
    long id,
    long categoryId,
    String path,
    Integer width,
    Integer height,
    Long bytes,
    String mimeType,
    String imageType,
    int position,
    boolean isPrimary
) {}
