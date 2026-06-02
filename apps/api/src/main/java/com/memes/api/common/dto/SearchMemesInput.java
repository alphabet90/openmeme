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
public class SearchMemesInput extends PaginationDto {
    private final String query;

    public SearchMemesInput(String query, int page, int limit, String locale) {
        super(page, limit, locale);
        this.query = query;
    }
}
