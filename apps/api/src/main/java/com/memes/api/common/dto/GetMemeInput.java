package com.memes.api.common.dto;

import lombok.Data;

@Data
public class GetMemeInput {
    private final String category;
    private final String slug;
    private final String locale;
}
