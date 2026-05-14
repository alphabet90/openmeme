package com.memes.api.service;

import com.memes.api.config.RedisConfig;
import com.memes.api.generated.model.CategoryPage;
import com.memes.api.generated.model.CategorySummary;
import com.memes.api.generated.model.CategoryTranslation;
import com.memes.api.generated.model.LocaleCode;
import com.memes.api.generated.model.Meme;
import com.memes.api.generated.model.MemeImage;
import com.memes.api.generated.model.MemeListItem;
import com.memes.api.generated.model.MemePage;
import com.memes.api.generated.model.MemeTranslation;
import com.memes.api.generated.model.SearchResult;
import com.memes.api.generated.model.Stats;
import com.memes.api.repository.CategoryRow;
import com.memes.api.repository.CategoryTranslationRow;
import com.memes.api.repository.MemeImageRow;
import com.memes.api.repository.MemeListItemRow;
import com.memes.api.repository.MemeRepository;
import com.memes.api.repository.MemeRepository.SearchHit;
import com.memes.api.repository.MemeRow;
import com.memes.api.repository.MemeTranslationRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemeService {

    private final MemeRepository memeRepository;

    @Value("${memes.cdn-url:}")
    private String cdnUrl;

    @Cacheable(value = RedisConfig.CACHE_STATS)
    public Stats getStats() {
        Stats stats = new Stats();
        memeRepository.findStats().ifPresent(row -> {
            stats.setTotalMemes((int) row.totalMemes());
            stats.setTotalCategories((int) row.totalCategories());
            stats.setTotalSubreddits((int) row.totalSubreddits());
            Optional.ofNullable(row.topCategory()).ifPresent(stats::setTopCategory);
            Optional.ofNullable(row.indexedAt()).ifPresent(stats::setIndexedAt);
        });
        return stats;
    }

    @Cacheable(value = RedisConfig.CACHE_CATEGORIES, key = "#locale + '-' + #page + '-' + #limit")
    public CategoryPage listCategories(String locale, int page, int limit) {
        int offset = page * limit;
        List<CategoryRow> rows = memeRepository.findAllCategories(locale, offset, limit);
        int total = memeRepository.countCategories();
        CategoryPage cp = new CategoryPage();
        cp.setData(rows.stream().map(this::toCategorySummary).toList());
        cp.setPage(page);
        cp.setLimit(limit);
        cp.setTotal(total);
        cp.setTotalPages(limit > 0 ? (int) Math.ceil((double) total / limit) : 0);
        return cp;
    }

    @Cacheable(value = RedisConfig.CACHE_MEME_LIST,
               key = "#page + '-' + #limit + '-' + #category + '-' + #sort + '-' + #locale")
    public MemePage listMemes(int page, int limit,
                              @Nullable String category,
                              String sort,
                              String locale) {
        int offset = page * limit;
        List<MemeListItemRow> rows = memeRepository.findAll(offset, limit, category, sort, locale);
        int total = memeRepository.countOptimized(category, locale);
        return toMemePage(rows, page, limit, total);
    }

    @Cacheable(value = RedisConfig.CACHE_MEME, key = "#category + '/' + #slug + '-' + #locale")
    public Optional<Meme> getMeme(String category, String slug, String locale) {
        return memeRepository.findBySlugAndCategory(slug, category)
            .map(r -> toMeme(r, locale));
    }

    @Cacheable(value = RedisConfig.CACHE_SEARCH,
               key = "#query + '-' + #page + '-' + #limit + '-' + #locale")
    public List<SearchResult> search(String query, int page, int limit, String locale) {
        int offset = page * limit;
        List<SearchHit> hits = memeRepository.search(query, locale, limit, offset);
        return hits.stream().map(h -> toSearchResult(h.meme(), locale)).toList();
    }

    private SearchResult toSearchResult(MemeRow row, String locale) {
        SearchResult result = new SearchResult();
        result.setSlug(row.slug());
        result.setCategory(row.categorySlug());
        Optional.ofNullable(row.authorUsername()).ifPresent(result::setAuthor);
        result.setScore(row.score());
        result.setTags(row.tagSlugs());
        row.translations().stream()
            .filter(t -> locale.equals(t.locale()))
            .findFirst()
            .or(() -> row.translations().stream()
                .filter(t -> row.defaultLocale().equals(t.locale()))
                .findFirst())
            .or(() -> row.translations().stream().findFirst())
            .ifPresent(t -> {
                result.setTitle(t.title());
                Optional.ofNullable(t.description()).ifPresent(result::setDescription);
            });
        row.images().stream()
            .filter(MemeImageRow::isPrimary)
            .findFirst()
            .or(() -> row.images().stream().findFirst())
            .map(MemeImageRow::path)
            .map(this::resolveImageUrl)
            .ifPresent(result::setImagePath);
        return result;
    }

    private MemePage toMemePage(List<MemeListItemRow> rows, int page, int limit, int total) {
        MemePage mp = new MemePage();
        mp.setData(rows.stream().map(this::toMemeListItem).toList());
        mp.setPage(page);
        mp.setLimit(limit);
        mp.setTotal(total);
        mp.setTotalPages(limit > 0 ? (int) Math.ceil((double) total / limit) : 0);
        return mp;
    }

    private MemeListItem toMemeListItem(MemeListItemRow r) {
        MemeListItem item = new MemeListItem();
        item.setSlug(r.slug());
        item.setScore(r.score());
        Optional.ofNullable(r.createdAt()).ifPresent(item::setCreatedAt);
        item.setCategory(r.categorySlug());
        item.setAuthor(r.authorUsername());
        item.setTitle(r.title());
        item.setDescription(r.description());
        item.setImagePath(resolveImageUrl(r.imagePath()));
        item.setTags(r.tagSlugs());
        return item;
    }

    private Meme toMeme(MemeRow r, String locale) {
        Meme m = new Meme();
        m.setSlug(r.slug());
        m.setCategory(r.categorySlug());
        m.setDefaultLocale(toLocaleCode(r.defaultLocale()));
        m.setAuthor(r.authorUsername());
        m.setSubreddit(r.subredditName());
        m.setScore(r.score());
        Optional.ofNullable(r.createdAt()).ifPresent(m::setCreatedAt);
        m.setSourceUrl(r.sourceUrl());
        m.setPostUrl(r.postUrl());
        m.setTranslations(r.translations().stream()
            .filter(t -> locale.equals(t.locale()))
            .map(this::toTranslation).toList());
        m.setImages(r.images().stream().map(this::toImage).toList());
        m.setTags(r.tagSlugs());
        return m;
    }

    private MemeTranslation toTranslation(MemeTranslationRow t) {
        MemeTranslation out = new MemeTranslation();
        out.setLocale(toLocaleCode(t.locale()));
        out.setTitle(t.title());
        out.setDescription(t.description());
        return out;
    }

    private MemeImage toImage(MemeImageRow img) {
        MemeImage out = new MemeImage();
        out.setPath(resolveImageUrl(img.path()));
        out.setWidth(img.width());
        out.setHeight(img.height());
        out.setBytes(img.bytes());
        out.setMimeType(img.mimeType());
        out.setPosition(img.position());
        out.setIsPrimary(img.isPrimary());
        return out;
    }

    private CategorySummary toCategorySummary(CategoryRow row) {
        CategorySummary cs = new CategorySummary();
        cs.setCategory(row.slug());
        cs.setCount(row.count());
        cs.setTopScore(row.topScore());
        cs.setTranslations(row.translations().stream()
            .map(this::toCategoryTranslation)
            .toList());
        return cs;
    }

    private CategoryTranslation toCategoryTranslation(CategoryTranslationRow t) {
        CategoryTranslation out = new CategoryTranslation();
        out.setLocale(toLocaleCode(t.locale()));
        out.setName(t.name());
        out.setDescription(t.description());
        return out;
    }

    private static LocaleCode toLocaleCode(@Nullable String value) {
        return Optional.ofNullable(value)
            .flatMap(v -> Optional.ofNullable(LocaleCode.fromValue(v)))
            .orElse(LocaleCode.EN);
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
