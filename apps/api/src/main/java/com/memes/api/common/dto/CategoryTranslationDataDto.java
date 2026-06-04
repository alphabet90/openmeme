package com.memes.api.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryTranslationDataDto {
    private String name;
    private String description;
    private List<String> tags;
}
