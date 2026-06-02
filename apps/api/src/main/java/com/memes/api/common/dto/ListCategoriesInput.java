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
public class ListCategoriesInput extends PaginationDto {

    public ListCategoriesInput(int page, int limit, String locale) {
        super(page, limit, locale);
    }
}
