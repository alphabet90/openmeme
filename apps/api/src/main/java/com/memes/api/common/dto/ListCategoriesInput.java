package com.memes.api.common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ListCategoriesInput extends PaginationDto {

    public ListCategoriesInput(int page, int limit, String locale) {
        super(page, limit, locale);
    }
}
