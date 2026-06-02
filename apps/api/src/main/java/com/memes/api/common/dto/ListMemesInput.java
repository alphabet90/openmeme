package com.memes.api.common.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
public class ListMemesInput extends PaginationDto {
    private final String category;
    private final String sort;
}
