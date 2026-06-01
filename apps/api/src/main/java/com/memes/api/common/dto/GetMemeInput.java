package com.memes.api.common.dto;

import lombok.Value;

@Value
public class GetMemeInput {
    String category;
    String slug;
    String locale;
}
