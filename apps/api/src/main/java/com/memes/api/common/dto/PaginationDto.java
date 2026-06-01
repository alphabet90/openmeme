package com.memes.api.common.dto;

import lombok.Data;

@Data
public class PaginationDto {
    private final int page;
    private final int limit;
    private final String locale;
}
