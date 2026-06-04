package com.memes.api.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaginationDto {
    private final int page;
    private final int limit;
    private final String locale;
    private final String sort;
    private final String category;
    private final String query;
}
