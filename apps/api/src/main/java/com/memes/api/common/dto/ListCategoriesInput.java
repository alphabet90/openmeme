package com.memes.api.common.dto;

import lombok.Value;

@Value
public class ListCategoriesInput {
    int page;
    int limit;
    String locale;
}
