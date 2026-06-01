package com.memes.api.common.dto;

import lombok.Value;

@Value
public class SearchMemesInput {
    String query;
    int page;
    int limit;
    String locale;
}
