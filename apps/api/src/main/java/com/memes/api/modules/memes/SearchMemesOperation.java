package com.memes.api.modules.memes;

import com.memes.api.common.dto.SearchMemesInput;
import com.memes.api.common.constants.CacheNames;
import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.SearchResult;
import com.memes.api.mappers.custom.MemeSearchMapper;
import com.memes.api.repository.JsonAggregates;
import com.memes.api.repository.MemeImageRow;
import com.memes.api.repository.MemeTranslationRow;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchMemesOperation implements Operation<SearchMemesInput, List<SearchResult>> {

    private final MemeSearchMapper memeSearchMapper;

    @Value("${memes.cdn-url:}")
    private String cdnUrl;

    @Override
    @Cacheable(value = CacheNames.SEARCH,
               key = "#input.query + '-' + #input.page + '-' + #input.limit + '-' + #input.locale")
    public List<SearchResult> execute(SearchMemesInput input) {
        int offset = input.getPage() * input.getLimit();
        List<Map<String, Object>> hits = memeSearchMapper.searchMemes(
            input.getQuery(), input.getLocale(), input.getLimit(), offset);
        if (hits.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> entries = hits.stream()
            .map(h -> Map.of(
                "category", (String) h.get("category"),
                "slug", (String) h.get("slug")))
            .toList();
        List<Map<String, Object>> details = memeSearchMapper.selectMemeDetailsBatch(entries);
        Map<String, Map<String, Object>> detailsByKey = details.stream()
            .filter(d -> d.get("category_slug") != null && d.get("slug") != null)
            .collect(Collectors.toMap(
                d -> d.get("category_slug") + ":" + d.get("slug"),
                d -> d,
                (a, b) -> a));
        return hits.stream()
            .map(h -> {
                String slug = (String) h.get("slug");
                String category = (String) h.get("category");
                Map<String, Object> detail = detailsByKey.get(category + ":" + slug);
                if (detail == null || detail.isEmpty()) {
                    return toSearchResultFromHit(h);
                }
                return toSearchResult(detail, input.getLocale());
            })
            .toList();
    }

    @SuppressWarnings("unchecked")
    private SearchResult toSearchResultFromHit(Map<String, Object> hit) {
        SearchResult result = new SearchResult();
        result.setSlug((String) hit.get("slug"));
        result.setCategory((String) hit.get("category"));
        result.setScore(hit.get("score") instanceof Number n ? n.intValue() : 0);
        result.setTitle((String) hit.get("title"));
        result.setDescription((String) hit.get("description"));
        return result;
    }

    @SuppressWarnings("unchecked")
    private SearchResult toSearchResult(Map<String, Object> detail, String locale) {
        SearchResult result = new SearchResult();
        result.setSlug((String) detail.get("slug"));
        result.setCategory((String) detail.get("category_slug"));
        Optional.ofNullable((String) detail.get("author_username")).ifPresent(result::setAuthor);
        result.setScore(detail.get("score") instanceof Number n ? n.intValue() : 0);

        Object tags = detail.get("tag_slugs");
        if (tags instanceof String[] arr) {
            result.setTags(Arrays.asList(arr));
        } else if (tags instanceof List<?> list) {
            result.setTags((List<String>) list);
        } else {
            result.setTags(List.of());
        }

        String defaultLocale = (String) detail.get("default_locale");
        String translationsJson = (String) detail.get("translations_json");
        var translations = translationsJson != null
            ? JsonAggregates.parseTranslations(translationsJson)
            : List.<MemeTranslationRow>of();

        translations.stream()
            .filter(t -> locale != null && locale.equals(t.locale()))
            .findFirst()
            .or(() -> translations.stream()
                .filter(t -> defaultLocale != null && defaultLocale.equals(t.locale()))
                .findFirst())
            .or(() -> translations.stream().findFirst())
            .ifPresent(t -> {
                result.setTitle(t.title());
                Optional.ofNullable(t.description()).ifPresent(result::setDescription);
            });

        String imagesJson = (String) detail.get("images_json");
        var images = imagesJson != null
            ? JsonAggregates.parseImages(imagesJson)
            : List.<MemeImageRow>of();
        images.stream()
            .filter(MemeImageRow::isPrimary)
            .findFirst()
            .or(() -> images.stream().findFirst())
            .map(MemeImageRow::path)
            .map(this::resolveImageUrl)
            .ifPresent(result::setImagePath);

        return result;
    }

    private String resolveImageUrl(String relativePath) {
        if (!StringUtils.hasText(cdnUrl) || !StringUtils.hasText(relativePath)) {
            return relativePath;
        }
        String base = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
        String path = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return base + "/" + path;
    }
}
