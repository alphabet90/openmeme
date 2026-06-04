package com.memes.api.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetMemeInput {
    private final String category;
    private final String slug;
    private final String locale;
}
