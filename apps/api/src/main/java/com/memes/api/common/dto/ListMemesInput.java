package com.memes.api.common.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
public class ListMemesInput extends PaginationDto {
    private final String category;
    private final String sort;

    public ListMemesInput(int page, int limit, String category, String sort, String locale) {
        super(page, limit, locale);
        this.category = category;
        this.sort = sort;
    }
}
