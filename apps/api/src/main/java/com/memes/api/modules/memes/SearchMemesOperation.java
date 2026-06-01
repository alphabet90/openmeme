package com.memes.api.modules.memes;

import com.memes.api.common.dto.SearchMemesInput;
import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.SearchResult;
import com.memes.api.mappers.custom.MemeSearchMapper;
import com.memes.api.models.MemeDetail;
import com.memes.api.models.SearchHit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchMemesOperation implements Operation<SearchMemesInput, List<SearchResult>> {

    private final MemeSearchMapper memeSearchMapper;

    @Value("${memes.cdn-url:}")
    private String cdnUrl;

    @Override
    public List<SearchResult> execute(SearchMemesInput input) {
        if (input.getQuery() == null || input.getQuery().isBlank()) return List.of();
        int offset = input.getPage() * input.getLimit();
        List<SearchHit> hits = memeSearchMapper.searchMemes(
            input.getQuery(), input.getLocale(), input.getLimit(), offset);
        return hits.stream().map(h -> toSearchResult(h, input.getLocale())).toList();
    }

    private SearchResult toSearchResult(SearchHit hit, String locale) {
        SearchResult result = new SearchResult();
        result.setSlug(hit.getSlug());
        result.setCategory(hit.getCategory());
        Optional.ofNullable(hit.getTitle()).ifPresent(result::setTitle);
        Optional.ofNullable(hit.getDescription()).ifPresent(result::setDescription);
        result.setScore(hit.getScore());
        return result;
    }
}
