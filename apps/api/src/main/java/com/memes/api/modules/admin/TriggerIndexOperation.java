package com.memes.api.modules.admin;

import com.memes.api.common.dto.IndexMemeInput;
import com.memes.api.common.dto.IndexResult;
import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.LocaleCode;
import com.memes.api.generated.model.MemeImage;
import com.memes.api.generated.model.MemeIndexRequest;
import com.memes.api.generated.model.MemeTranslation;
import com.memes.api.mappers.CategoryMapper;
import com.memes.api.mappers.MemeMapper;
import com.memes.api.mappers.custom.MemeWriteMapper;
import com.memes.api.models.Meme;
import com.memes.api.repository.CategoryImageRow;
import com.memes.api.repository.MemeImageRow;
import com.memes.api.repository.MemeTranslationRow;
import com.memes.api.repository.MemeUpsert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class TriggerIndexOperation implements Operation<IndexMemeInput, IndexResult> {

    static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    static final Pattern URL_PATTERN = Pattern.compile("^(https://|/).+", Pattern.CASE_INSENSITIVE);

    private final MemeWriteMapper memeWriteMapper;
    private final MemeMapper memeMapper;
    private final CategoryMapper categoryMapper;
    private final InvalidateCachesOperation invalidateCachesOperation;

    @Override
    public IndexResult execute(IndexMemeInput input) {
        long start = System.currentTimeMillis();
        MemeIndexRequest body = input.getRequest();

        if (body != null && body.getSlug() != null && !body.getSlug().isBlank()) {
            return indexSingle(body, start);
        }
        throw new UnsupportedOperationException("Full filesystem reindex is no longer supported.");
    }

    @Async("reindexExecutor")
    public void executeAsync(IndexMemeInput input) {
        try {
            IndexResult result = execute(input);
            log.info("Async reindex done: indexed={} durationMs={} errors={}",
                result.getIndexed(), result.getDurationMs(), result.getErrors().size());
        } catch (Exception e) {
            log.error("Async reindex failed", e);
        }
    }

    private IndexResult indexSingle(MemeIndexRequest req, long start) {
        MemeUpsert upsert = fromIndexRequest(req);
        upsertMeme(upsert);
        memeWriteMapper.refreshStats();
        invalidateCachesOperation.invalidateAll();
        long duration = System.currentTimeMillis() - start;
        log.info("Single meme indexed: {}/{} in {}ms",
            upsert.categorySlug(), upsert.slug(), duration);
        return new IndexResult(1, duration, List.of());
    }

    // ===== DB write helpers ===================================================

    private long upsertCategory(CategoryUpsert cat) {
        long categoryId = categoryMapper.upsertReturningId(cat.slug());
        for (var entry : cat.translations().entrySet()) {
            CategoryTranslationData data = entry.getValue();
            memeWriteMapper.upsertCategoryTranslation(
                categoryId, entry.getKey(), data.name(), data.description());
        }
        memeWriteMapper.deleteCategoryImages(categoryId);
        for (CategoryImageRow img : cat.images()) {
            memeWriteMapper.insertCategoryImage(
                categoryId, img.path(), img.width(), img.height(),
                img.bytes(), img.mimeType(), img.imageType(), img.position(), img.isPrimary());
        }
        return categoryId;
    }

    private void upsertMeme(MemeUpsert upsert) {
        long categoryId = memeWriteMapper.upsertCategoryReturningId(upsert.categorySlug());
        Long subredditId = Optional.ofNullable(upsert.subredditName())
            .filter(s -> !s.isBlank())
            .map(memeWriteMapper::upsertSubredditReturningId)
            .orElse(null);
        Long authorId = Optional.ofNullable(upsert.authorUsername())
            .filter(s -> !s.isBlank())
            .map(memeWriteMapper::upsertAuthorReturningId)
            .orElse(null);

        Meme meme = new Meme();
        meme.setCategoryId(categoryId);
        meme.setSlug(upsert.slug());
        meme.setSubredditId(subredditId);
        meme.setAuthorId(authorId);
        meme.setDefaultLocale(upsert.defaultLocale());
        meme.setScore(upsert.score());
        meme.setSourceUrl(upsert.sourceUrl());
        meme.setPostUrl(upsert.postUrl());
        meme.setCreatedAt(upsert.createdAt());
        memeMapper.upsert(meme);
        long memeId = meme.getId();

        memeWriteMapper.deleteMemeImages(memeId);
        for (MemeImageRow img : upsert.images()) {
            memeWriteMapper.insertMemeImage(
                memeId, img.path(), img.width(), img.height(),
                img.bytes(), img.mimeType(), img.position(), img.isPrimary());
        }

        memeWriteMapper.deleteMemeTags(memeId);
        for (String tagSlug : upsert.tagSlugs()) {
            long tagId = memeWriteMapper.upsertTagReturningId(tagSlug);
            memeWriteMapper.insertMemeTag(memeId, tagId);
        }

        for (MemeTranslationRow t : upsert.translations()) {
            memeWriteMapper.upsertMemeTranslation(memeId, t.locale(), t.title(), t.description());
        }
    }

    // ===== From admin API =====================================================

    private MemeUpsert fromIndexRequest(MemeIndexRequest req) {
        String category = Optional.ofNullable(req.getCategory()).orElse("");
        String slug = Optional.ofNullable(req.getSlug()).orElse("");
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new IllegalArgumentException("slug violates slug domain: " + slug);
        }
        if (!SLUG_PATTERN.matcher(category).matches()) {
            throw new IllegalArgumentException("category violates slug domain: " + category);
        }
        LocaleCode defaultLocaleEnum = Optional.ofNullable(req.getDefaultLocale()).orElse(LocaleCode.EN);
        String defaultLocale = defaultLocaleEnum.getValue();

        List<MemeTranslationRow> translations = Optional.ofNullable(req.getTranslations())
            .orElse(List.of())
            .stream()
            .map(TriggerIndexOperation::toTranslationRow)
            .toList();
        if (translations.isEmpty()) {
            throw new IllegalArgumentException("translations[] is required");
        }
        boolean hasDefault = translations.stream().anyMatch(t -> defaultLocale.equals(t.locale()));
        if (!hasDefault) {
            throw new IllegalArgumentException(
                "no translation for default_locale '" + defaultLocale + "'");
        }

        List<MemeImageRow> images = Optional.ofNullable(req.getImages())
            .filter(l -> !l.isEmpty())
            .map(list -> list.stream().map(TriggerIndexOperation::toImageRow).toList())
            .orElseGet(() -> List.of(MemeImageRow.builder()
                .path("memes/" + category + "/" + slug + ".jpg")
                .position(0)
                .isPrimary(true)
                .build()));

        List<String> tags = Optional.ofNullable(req.getTags()).orElse(List.of());

        return MemeUpsert.builder()
            .slug(slug)
            .categorySlug(category)
            .defaultLocale(defaultLocale)
            .subredditName(sanitizeSubreddit(req.getSubreddit()))
            .authorUsername(sanitizeAuthor(req.getAuthor()))
            .score(Optional.ofNullable(req.getScore()).orElse(0))
            .createdAt(Optional.ofNullable(req.getCreatedAt()).orElse(null))
            .sourceUrl(sanitizeUrl(req.getSourceUrl(), "source_url"))
            .postUrl(sanitizeUrl(req.getPostUrl(), "post_url"))
            .translations(translations)
            .images(images)
            .tagSlugs(tags)
            .build();
    }

    private static MemeTranslationRow toTranslationRow(MemeTranslation t) {
        return MemeTranslationRow.builder()
            .locale(t.getLocale().getValue())
            .title(t.getTitle())
            .description(t.getDescription())
            .build();
    }

    private static MemeImageRow toImageRow(MemeImage img) {
        return MemeImageRow.builder()
            .path(img.getPath())
            .width(img.getWidth())
            .height(img.getHeight())
            .bytes(img.getBytes())
            .mimeType(img.getMimeType())
            .position(Optional.ofNullable(img.getPosition()).orElse(0))
            .isPrimary(Boolean.TRUE.equals(img.getIsPrimary()))
            .build();
    }

    // ===== Helpers ============================================================

    private String sanitizeUrl(String raw, String field) {
        if (raw == null || raw.isBlank()) return null;
        if (URL_PATTERN.matcher(raw).matches() && raw.length() <= 2048) return raw;
        log.warn("Dropping invalid {} '{}'", field, raw);
        return null;
    }

    private String sanitizeSubreddit(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.matches("^[A-Za-z0-9_]{1,21}$")) return raw;
        log.warn("Dropping invalid subreddit '{}'", raw);
        return null;
    }

    private String sanitizeAuthor(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if ("[deleted]".equals(raw) || "[removed]".equals(raw)) return raw;
        if (raw.matches("^[A-Za-z0-9_-]{1,20}$")) return raw;
        log.warn("Dropping invalid author '{}'", raw);
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseTags(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            String s = Optional.ofNullable(o).map(Object::toString).orElse("").trim();
            if (s.isEmpty()) continue;
            if (!SLUG_PATTERN.matcher(s).matches()) {
                log.warn("Dropping invalid tag '{}'", s);
                continue;
            }
            out.add(s);
        }
        return out;
    }

    private OffsetDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s);
        } catch (Exception e) {
            log.warn("Could not parse datetime: {}", s);
            return null;
        }
    }

    private String str(Map<String, Object> fm, String key) {
        return Optional.ofNullable(fm.get(key)).map(Object::toString).orElse(null);
    }

    private String strFromMap(Map<Object, Object> map, String key) {
        return Optional.ofNullable(map.get(key)).map(Object::toString).orElse(null);
    }

    private int toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private Integer intOrNull(Object v) {
        if (v instanceof Number n) return n.intValue();
        return null;
    }

    private Long longOrNull(Object v) {
        if (v instanceof Number n) return n.longValue();
        return null;
    }

    public record CategoryTranslationData(String name, String description, List<String> tags) {}

    public record CategoryUpsert(
        String slug,
        String defaultLocale,
        Map<String, CategoryTranslationData> translations,
        List<CategoryImageRow> images
    ) {}
}
