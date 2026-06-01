package com.memes.api.common.dto;

import org.springframework.lang.Nullable;

public record ListMemesInput(
    int page,
    int limit,
    @Nullable String category,
    String sort,
    String locale
) {}
