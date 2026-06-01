package com.memes.api.common.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.lang.Nullable;

@Value
@Builder
public class ListMemesInput {
    int page;
    int limit;
    @Nullable String category;
    String sort;
    String locale;
}
