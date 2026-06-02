package com.memes.api.modules.memes;

import com.memes.api.common.dto.ListCategoriesInput;
import com.memes.api.common.constants.CacheNames;
import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.CategoryImage;
import com.memes.api.generated.model.CategoryPage;
import com.memes.api.generated.model.CategorySummary;
import com.memes.api.generated.model.CategoryTranslation;
import com.memes.api.generated.model.LocaleCode;
import com.memes.api.mappers.custom.CategoryQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListCategoriesOperation implements Operation<ListCategoriesInput, CategoryPage> {

    private final CategoryQueryMapper categoryQueryMapper;

    @Override
    @Cacheable(value = CacheNames.CATEGORIES, key = "#input.locale + '-' + #input.page + '-' + #input.limit")
    public CategoryPage execute(ListCategoriesInput input) {
        int offset = input.getPage() * input.getLimit();
        List<Map<String, Object>> flatRows = categoryQueryMapper.selectCategoriesWithTranslations(
            input.getLocale(), input.getLimit(), offset);
        int total = categoryQueryMapper.countCategories();
        CategoryPage cp = new CategoryPage();
        cp.setData(aggregateCategories(flatRows).stream().map(this::toCategorySummary).toList());
        cp.setPage(input.getPage());
        cp.setLimit(input.getLimit());
        cp.setTotal(total);
        cp.setTotalPages(input.getLimit() > 0 ? (int) Math.ceil((double) total / input.getLimit()) : 0);
        return cp;
    }

    private List<CategoryView> aggregateCategories(List<Map<String, Object>> flatRows) {
        Map<Long, CategoryView> map = new LinkedHashMap<>();
        List<Long> allIds = flatRows.stream()
            .map(row -> ((Number) row.get("category_id")).longValue())
            .distinct()
            .toList();
        Map<Long, List<Map<String, Object>>> imagesByCategory = categoryQueryMapper
            .selectCategoryImagesByBatch(allIds)
            .stream()
            .collect(Collectors.groupingBy(img -> ((Number) img.get("category_id")).longValue()));
        for (Map<String, Object> row : flatRows) {
            long categoryId = ((Number) row.get("category_id")).longValue();
            CategoryView cv = map.computeIfAbsent(categoryId, id -> {
                List<Map<String, Object>> images = imagesByCategory.getOrDefault(id, List.of());
                return new CategoryView(
                    id,
                    (String) row.get("category"),
                    ((Number) row.get("count")).intValue(),
                    ((Number) row.get("top_score")).intValue(),
                    new ArrayList<>(),
                    images
                );
            });
            cv.translations().add(Map.of(
                "locale", (String) row.get("locale"),
                "name", (String) row.get("name"),
                "description", (String) row.get("description")
            ));
        }
        return List.copyOf(map.values());
    }

    private CategorySummary toCategorySummary(CategoryView cv) {
        CategorySummary cs = new CategorySummary();
        cs.setCategory(cv.slug());
        cs.setCount(cv.count());
        cs.setTopScore(cv.topScore());
        cs.setTranslations(cv.translations().stream()
            .map(t -> new CategoryTranslation()
                .locale(toLocaleCode((String) t.get("locale")))
                .name((String) t.get("name"))
                .description((String) t.get("description")))
            .toList());
        if (cv.images() != null && !cv.images().isEmpty()) {
            cs.setImages(cv.images().stream().map(this::toCategoryImage).toList());
        }
        return cs;
    }

    private CategoryImage toCategoryImage(Map<String, Object> row) {
        return new CategoryImage()
            .path((String) row.get("path"))
            .width(row.get("width") instanceof Number n ? n.intValue() : null)
            .height(row.get("height") instanceof Number n ? n.intValue() : null)
            .bytes(row.get("bytes") instanceof Number n ? n.longValue() : null)
            .mimeType((String) row.get("mime_type"))
            .imageType(CategoryImage.ImageTypeEnum.fromValue((String) row.get("image_type")))
            .position(row.get("position") instanceof Number n ? n.intValue() : 0)
            .isPrimary(row.get("is_primary") instanceof Boolean b ? b : false);
    }

    private static LocaleCode toLocaleCode(@Nullable String value) {
        return Optional.ofNullable(value)
            .flatMap(v -> Optional.ofNullable(LocaleCode.fromValue(v)))
            .orElse(LocaleCode.EN);
    }

    private record CategoryView(long id, String slug, int count, int topScore,
                                 List<Map<String, Object>> translations,
                                 List<Map<String, Object>> images) {}
}
