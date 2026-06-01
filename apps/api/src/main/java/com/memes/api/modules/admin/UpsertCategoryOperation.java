package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.mappers.CategoryMapper;
import com.memes.api.mappers.custom.MemeSearchMapper;
import com.memes.api.models.Category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpsertCategoryOperation implements Operation<CategoryUpsert, Void> {

    private final CategoryMapper categoryMapper;
    private final MemeSearchMapper memeSearchMapper;

    @Override
    @Transactional
    public Void execute(CategoryUpsert u) {
        Category cat = new Category();
        cat.setSlug(u.slug());
        categoryMapper.upsert(cat);
        long categoryId = Optional.ofNullable(cat.getId())
            .orElseThrow(() -> new IllegalStateException("Category upsert returned null id for " + u.slug()));

        if (u.translations() != null) {
            for (var entry : u.translations().entrySet()) {
                String locale = entry.getKey();
                var data = entry.getValue();
                memeSearchMapper.upsertCategoryTranslation(categoryId, locale, data.name(), data.description());
            }
        }

        memeSearchMapper.deleteCategoryImages(categoryId);
        if (u.images() != null) {
            for (var img : u.images()) {
                memeSearchMapper.insertCategoryImage(categoryId, img.path(), img.width(), img.height(),
                    img.bytes(), img.mimeType(), img.imageType(), img.position(), img.isPrimary());
            }
        }

        return null;
    }
}
