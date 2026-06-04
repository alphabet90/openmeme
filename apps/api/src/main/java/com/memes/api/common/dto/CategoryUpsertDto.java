package com.memes.api.common.dto;

import com.memes.api.repository.CategoryImageRow;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CategoryUpsertDto {
    private String slug;
    private String defaultLocale;
    private Map<String, CategoryTranslationDataDto> translations;
    private List<CategoryImageRow> images;
}
