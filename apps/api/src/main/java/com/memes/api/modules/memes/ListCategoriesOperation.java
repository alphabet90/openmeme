package com.memes.api.modules.memes;

import com.memes.api.common.dto.ListCategoriesInput;
import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.CategoryImage;
import com.memes.api.generated.model.CategoryPage;
import com.memes.api.generated.model.CategorySummary;
import com.memes.api.generated.model.CategoryTranslation;
import com.memes.api.generated.model.LocaleCode;
import com.memes.api.mappers.custom.MemeSearchMapper;
import com.memes.api.models.CategoryDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ListCategoriesOperation implements Operation<ListCategoriesInput, CategoryPage> {

    private final MemeSearchMapper memeSearchMapper;

    @Override
    public CategoryPage execute(ListCategoriesInput input) {
        int offset = input.getPage() * input.getLimit();
        List<CategoryDetail> details = memeSearchMapper.selectCategories(input.getLocale(), offset, input.getLimit());
        int total = memeSearchMapper.countCategories();
        CategoryPage cp = new CategoryPage();
        cp.setData(details.stream().map(this::toCategorySummary).toList());
        cp.setPage(input.getPage());
        cp.setLimit(input.getLimit());
        cp.setTotal(total);
        cp.setTotalPages(input.getLimit() > 0 ? (int) Math.ceil((double) total / input.getLimit()) : 0);
        return cp;
    }

    private CategorySummary toCategorySummary(CategoryDetail row) {
        CategorySummary cs = new CategorySummary();
        cs.setCategory(row.getSlug());
        cs.setCount(row.getCount());
        cs.setTopScore(row.getTopScore());
        cs.setTranslations(row.getTranslations().stream()
            .map(this::toCategoryTranslation)
            .toList());
        if (row.getImages() != null && !row.getImages().isEmpty()) {
            cs.setImages(row.getImages().stream()
                .map(this::toCategoryImage)
                .toList());
        }
        return cs;
    }

    private CategoryTranslation toCategoryTranslation(com.memes.api.models.CategoryTranslation t) {
        CategoryTranslation out = new CategoryTranslation();
        out.setLocale(toLocaleCode(t.getLocale()));
        out.setName(t.getName());
        out.setDescription(t.getDescription());
        return out;
    }

    private CategoryImage toCategoryImage(com.memes.api.models.CategoryImage row) {
        CategoryImage img = new CategoryImage();
        img.setPath(row.getPath());
        img.setWidth(row.getWidth());
        img.setHeight(row.getHeight());
        img.setBytes(row.getBytes());
        img.setMimeType(row.getMimeType());
        img.setImageType(CategoryImage.ImageTypeEnum.fromValue(row.getImageType()));
        img.setPosition(row.getPosition());
        img.setIsPrimary(row.getIsPrimary());
        return img;
    }

    private static LocaleCode toLocaleCode(String value) {
        return Optional.ofNullable(value)
            .flatMap(v -> Optional.ofNullable(LocaleCode.fromValue(v)))
            .orElse(LocaleCode.EN);
    }
}
