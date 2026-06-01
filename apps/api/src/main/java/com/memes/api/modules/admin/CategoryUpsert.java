package com.memes.api.modules.admin;

import com.memes.api.modules.admin.CategoryImageRow;

import java.util.List;
import java.util.Map;

public record CategoryUpsert(
    String slug,
    String defaultLocale,
    Map<String, CategoryTranslationData> translations,
    List<CategoryImageRow> images
) {}
