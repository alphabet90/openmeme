package com.memes.api.controllers;

import com.memes.api.common.dto.GetMemeInput;
import com.memes.api.common.dto.GetStatsInput;
import com.memes.api.common.dto.ListCategoriesInput;
import com.memes.api.common.dto.ListMemesInput;
import com.memes.api.common.dto.SearchMemesInput;
import com.memes.api.generated.api.MemesApiDelegate;
import com.memes.api.generated.model.CategoryPage;
import com.memes.api.generated.model.LocaleCode;
import com.memes.api.generated.model.Meme;
import com.memes.api.generated.model.MemePage;
import com.memes.api.generated.model.SearchResult;
import com.memes.api.generated.model.Stats;
import com.memes.api.modules.memes.GetMemeOperation;
import com.memes.api.modules.memes.GetStatsOperation;
import com.memes.api.modules.memes.ListCategoriesOperation;
import com.memes.api.modules.memes.ListMemesOperation;
import com.memes.api.modules.memes.SearchMemesOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemesController implements MemesApiDelegate {

    private final GetStatsOperation getStatsOperation;
    private final ListCategoriesOperation listCategoriesOperation;
    private final ListMemesOperation listMemesOperation;
    private final GetMemeOperation getMemeOperation;
    private final SearchMemesOperation searchMemesOperation;

    @Override
    public ResponseEntity<Stats> getStats() {
        return ResponseEntity.ok(getStatsOperation.execute(new GetStatsInput()));
    }

    @Override
    public ResponseEntity<CategoryPage> listCategories(Integer page, Integer limit, LocaleCode locale) {
        return ResponseEntity.ok(listCategoriesOperation.execute(new ListCategoriesInput(
            Optional.ofNullable(page).orElse(0),
            Optional.ofNullable(limit).orElse(100),
            localeValue(locale)
        )));
    }

    @Override
    public ResponseEntity<MemePage> listMemes(
            Integer page, Integer limit,
            String category, String sort,
            LocaleCode locale) {
        return ResponseEntity.ok(listMemesOperation.execute(ListMemesInput.builder()
            .page(Optional.ofNullable(page).orElse(0))
            .limit(Optional.ofNullable(limit).orElse(100))
            .category(category)
            .sort(Optional.ofNullable(sort).orElse("score"))
            .locale(localeValue(locale))
            .build()));
    }

    @Override
    public ResponseEntity<MemePage> listMemesByCategory(
            String category, Integer page, Integer limit, String sort, LocaleCode locale) {
        MemePage result = listMemesOperation.execute(ListMemesInput.builder()
            .page(Optional.ofNullable(page).orElse(0))
            .limit(Optional.ofNullable(limit).orElse(100))
            .category(category)
            .sort(Optional.ofNullable(sort).orElse("score"))
            .locale(localeValue(locale))
            .build());
        return Optional.of(result)
            .filter(r -> r.getTotal() != null && r.getTotal() > 0)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Meme> getMeme(String category, String slug, LocaleCode locale) {
        return getMemeOperation.execute(new GetMemeInput(category, slug, localeValue(locale)))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<SearchResult>> searchMemes(String q, Integer page, Integer limit, LocaleCode locale) {
        return ResponseEntity.ok(searchMemesOperation.execute(new SearchMemesInput(
            q,
            Optional.ofNullable(page).orElse(0),
            Optional.ofNullable(limit).orElse(100),
            localeValue(locale)
        )));
    }

    private static String localeValue(@Nullable LocaleCode locale) {
        return Optional.ofNullable(locale).orElse(LocaleCode.EN).getValue();
    }
}
