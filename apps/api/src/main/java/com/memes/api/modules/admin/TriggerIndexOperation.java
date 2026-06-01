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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TriggerIndexOperation implements Operation<IndexMemeInput, IndexResult> {

    static final Set<String> ALLOWED_LOCALES = Set.of("en", "es", "pt", "fr", "de", "ar");
    static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    static final Pattern URL_PATTERN = Pattern.compile("^(https://|/).+", Pattern.CASE_INSENSITIVE);
    static final String DEFAULT_LOCALE = "en";

    static final Pattern LOCALE_MDX_PATTERN =
        Pattern.compile("^(.+)\\.([a-z]{2}(?:-[A-Z]{2})?)\\.mdx$");

    @Value("${memes.root}")
    private String memesRoot;

    private final MemeWriteMapper memeWriteMapper;
    private final MemeMapper memeMapper;
    private final CategoryMapper categoryMapper;
    private final InvalidateCachesOperation invalidateCachesOperation;

    @Override
    public IndexResult execute(IndexMemeInput input) {
        long start = System.currentTimeMillis();
        MemeIndexRequest body = input.request();

        if (body != null && body.getSlug() != null && !body.getSlug().isBlank()) {
            return indexSingle(body, start);
        }
        return reindex(input.indexMemes(), input.indexCategories(), start);
    }

    @Async("reindexExecutor")
    public void executeAsync(IndexMemeInput input) {
        try {
            IndexResult result = execute(input);
            log.info("Async reindex done: indexed={} durationMs={} errors={}",
                result.indexed(), result.durationMs(), result.errors().size());
        } catch (Exception e) {
            log.error("Async reindex failed", e);
        }
    }

    private IndexResult reindex(boolean indexMemes, boolean indexCategories, long start) {
        List<String> errors = new ArrayList<>();
        int categoryCount = 0;
        int memeCount = 0;

        if (indexCategories) {
            List<CategoryUpsert> categories = scanCategoryMdxFiles(errors);
            for (CategoryUpsert cat : categories) {
                upsertCategory(cat);
            }
            categoryCount = categories.size();
        }

        if (indexMemes) {
            List<MemeUpsert> memes = scanMdxFiles(errors);
            for (MemeUpsert upsert : memes) {
                try { upsertMeme(upsert); memeCount++; }
                catch (Exception e) { errors.add("Failed to upsert meme: " + e.getMessage()); }
            }
        }

        if (indexCategories) {
            purgeOrphanCategories(errors);
        }

        memeWriteMapper.refreshStats();
        invalidateCachesOperation.invalidateAll();
        long duration = System.currentTimeMillis() - start;
        log.info("Reindex complete: {} categories, {} memes indexed in {}ms ({} errors)",
            categoryCount, memeCount, duration, errors.size());
        return new IndexResult(memeCount, duration, errors);
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

    // ===== MDX scanning =======================================================

    private List<MemeUpsert> scanMdxFiles(List<String> errors) {
        Path root = Paths.get(memesRoot);
        if (!Files.isDirectory(root)) {
            errors.add("Memes root not found: " + root.toAbsolutePath());
            return Collections.emptyList();
        }

        Map<Path, List<Path>> groups = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            stream
                .filter(p -> p.toString().endsWith(".mdx") && Files.isRegularFile(p))
                .sorted()
                .forEach(p -> {
                    Path baseKey = toBaseKey(p);
                    groups.computeIfAbsent(baseKey, k -> new ArrayList<>()).add(p);
                });
        } catch (IOException e) {
            errors.add("Error walking memes directory: " + e.getMessage());
            return Collections.emptyList();
        }

        List<MemeUpsert> upserts = new ArrayList<>();
        for (Map.Entry<Path, List<Path>> entry : groups.entrySet()) {
            try {
                parseMdxGroup(entry.getKey(), entry.getValue()).ifPresent(upserts::add);
            } catch (Exception e) {
                String msg = "Failed to parse " + entry.getKey() + ": " + e.getMessage();
                log.warn(msg);
                errors.add(msg);
            }
        }
        return upserts;
    }

    private Path toBaseKey(Path mdxPath) {
        String name = mdxPath.getFileName().toString();
        Matcher m = LOCALE_MDX_PATTERN.matcher(name);
        if (m.matches()) {
            return mdxPath.getParent().resolve(m.group(1) + ".mdx");
        }
        return mdxPath;
    }

    Optional<MemeUpsert> parseMdxGroup(Path baseKey, List<Path> group) throws IOException {
        List<Path> sorted = group.stream()
            .sorted(Comparator.comparing(p -> p.equals(baseKey) ? 0 : 1))
            .toList();

        MemeUpsert skeleton = null;
        List<MemeTranslationRow> allTranslations = new ArrayList<>();
        Set<String> seenLocales = new HashSet<>();

        for (Path p : sorted) {
            Optional<MemeUpsert> parsed;
            try {
                parsed = parseMdx(p);
            } catch (Exception e) {
                log.warn("Failed to parse {}: {}", p, e.getMessage());
                continue;
            }
            if (parsed.isEmpty()) continue;

            MemeUpsert u = parsed.get();
            if (skeleton == null) skeleton = u;

            for (MemeTranslationRow t : u.translations()) {
                if (seenLocales.add(t.locale())) {
                    allTranslations.add(t);
                }
            }
        }

        if (skeleton == null || allTranslations.isEmpty()) return Optional.empty();

        String defaultLocale = skeleton.defaultLocale();
        boolean hasDefault = allTranslations.stream().anyMatch(t -> defaultLocale.equals(t.locale()));
        if (!hasDefault) {
            log.warn("Skipping {}: no translation for default_locale '{}'", baseKey, defaultLocale);
            return Optional.empty();
        }

        return Optional.of(MemeUpsert.builder()
            .slug(skeleton.slug())
            .categorySlug(skeleton.categorySlug())
            .defaultLocale(defaultLocale)
            .subredditName(skeleton.subredditName())
            .authorUsername(skeleton.authorUsername())
            .score(skeleton.score())
            .createdAt(skeleton.createdAt())
            .sourceUrl(skeleton.sourceUrl())
            .postUrl(skeleton.postUrl())
            .images(skeleton.images())
            .tagSlugs(skeleton.tagSlugs())
            .translations(allTranslations)
            .build());
    }

    @SuppressWarnings("unchecked")
    Optional<MemeUpsert> parseMdx(Path mdxPath) throws IOException {
        List<String> lines = Files.readAllLines(mdxPath);
        if (lines.isEmpty() || !"---".equals(lines.get(0).trim())) {
            return Optional.empty();
        }
        int end = -1;
        for (int i = 1; i < lines.size(); i++) {
            if ("---".equals(lines.get(i).trim())) { end = i; break; }
        }
        if (end < 0) return Optional.empty();

        String yamlBlock = String.join("\n", lines.subList(1, end));
        Map<String, Object> fm = new Yaml().load(yamlBlock);
        if (fm == null || fm.isEmpty()) return Optional.empty();

        String category = str(fm, "category");
        String slug = str(fm, "slug");
        if (category == null || category.isBlank() || slug == null || slug.isBlank()) {
            log.warn("Skipping {}: missing category or slug", mdxPath);
            return Optional.empty();
        }
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            log.warn("Skipping {}: slug '{}' violates slug domain", mdxPath, slug);
            return Optional.empty();
        }
        if (!SLUG_PATTERN.matcher(category).matches()) {
            log.warn("Skipping {}: category '{}' violates slug domain", mdxPath, category);
            return Optional.empty();
        }

        List<MemeTranslationRow> translations;
        String defaultLocale;

        if (fm.get("translations") instanceof Map<?, ?>) {
            defaultLocale = Optional.ofNullable(str(fm, "default_locale"))
                .filter(ALLOWED_LOCALES::contains)
                .orElse(DEFAULT_LOCALE);
            translations = parseTranslations(fm.get("translations"), mdxPath);
        } else {
            String title = str(fm, "title");
            if (title == null || title.isBlank()) {
                log.warn("Skipping {}: no translations block and no title field", mdxPath);
                return Optional.empty();
            }
            String fileLocale = extractLocaleFromFilename(mdxPath.getFileName().toString())
                .map(TriggerIndexOperation::normalizeLocale)
                .orElse(DEFAULT_LOCALE);
            defaultLocale = fileLocale;
            translations = List.of(MemeTranslationRow.builder()
                .locale(fileLocale)
                .title(title.trim())
                .description(str(fm, "description"))
                .build());
        }

        if (translations.isEmpty()) {
            log.warn("Skipping {}: no valid translations", mdxPath);
            return Optional.empty();
        }
        boolean hasDefault = translations.stream().anyMatch(t -> defaultLocale.equals(t.locale()));
        if (!hasDefault) {
            log.warn("Skipping {}: no translation for default_locale '{}'", mdxPath, defaultLocale);
            return Optional.empty();
        }

        List<MemeImageRow> images = parseImages(fm.get("images"), category, slug, mdxPath);
        List<String> tags = parseTags(fm.get("tags"));

        return Optional.of(MemeUpsert.builder()
            .slug(slug)
            .categorySlug(category)
            .defaultLocale(defaultLocale)
            .subredditName(sanitizeSubreddit(str(fm, "subreddit")))
            .authorUsername(sanitizeAuthor(str(fm, "author")))
            .score(toInt(fm.get("score")))
            .createdAt(parseDateTime(str(fm, "created_at")))
            .sourceUrl(sanitizeUrl(str(fm, "source_url"), mdxPath, "source_url"))
            .postUrl(sanitizeUrl(str(fm, "post_url"), mdxPath, "post_url"))
            .translations(translations)
            .images(images)
            .tagSlugs(tags)
            .build());
    }

    @SuppressWarnings("unchecked")
    private List<MemeTranslationRow> parseTranslations(Object raw, Path mdxPath) {
        if (!(raw instanceof Map<?, ?> map)) return List.of();
        List<MemeTranslationRow> out = new ArrayList<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String locale = String.valueOf(e.getKey());
            if (!ALLOWED_LOCALES.contains(locale)) {
                log.warn("Skipping unknown locale '{}' in {}", locale, mdxPath);
                continue;
            }
            if (!(e.getValue() instanceof Map<?, ?> body)) continue;
            String title = strFromMap((Map<Object, Object>) body, "title");
            if (title == null || title.isBlank()) {
                log.warn("Skipping locale '{}' in {}: missing title", locale, mdxPath);
                continue;
            }
            String description = strFromMap((Map<Object, Object>) body, "description");
            out.add(MemeTranslationRow.builder()
                .locale(locale)
                .title(title)
                .description(description)
                .build());
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<MemeImageRow> parseImages(Object raw, String category, String slug, Path mdxPath) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of(MemeImageRow.builder()
                .path(deriveDefaultImagePath(category, slug, mdxPath))
                .position(0)
                .isPrimary(true)
                .build());
        }
        List<MemeImageRow> out = new ArrayList<>();
        boolean primaryAssigned = false;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Map<?, ?> body)) continue;
            Map<Object, Object> m = (Map<Object, Object>) body;
            String path = normalizeImagePath(strFromMap(m, "path"), category, mdxPath, slug);
            if (path == null) continue;
            boolean isPrimary = Boolean.TRUE.equals(m.get("is_primary"));
            if (isPrimary && primaryAssigned) {
                isPrimary = false;
                log.warn("Multiple primary images in {}; keeping the first", mdxPath);
            }
            primaryAssigned = primaryAssigned || isPrimary;
            out.add(MemeImageRow.builder()
                .path(path)
                .width(intOrNull(m.get("width")))
                .height(intOrNull(m.get("height")))
                .bytes(longOrNull(m.get("bytes")))
                .mimeType(strFromMap(m, "mime_type"))
                .position(i)
                .isPrimary(isPrimary)
                .build());
        }
        if (!primaryAssigned && !out.isEmpty()) {
            MemeImageRow first = out.get(0);
            out.set(0, MemeImageRow.builder()
                .path(first.path())
                .width(first.width())
                .height(first.height())
                .bytes(first.bytes())
                .mimeType(first.mimeType())
                .position(first.position())
                .isPrimary(true)
                .build());
        }
        return out;
    }

    // ===== Category MDX scanning ==============================================

    private List<CategoryUpsert> scanCategoryMdxFiles(List<String> errors) {
        Path root = Paths.get(memesRoot);
        Path categoriesDir = root.resolve("_categories");
        if (!Files.isDirectory(categoriesDir)) {
            return List.of();
        }

        Pattern mdxFilePattern = Pattern.compile("^(.+?)(?:\\.([a-z]{2}(?:-[A-Z]{2})?))?\\.mdx$");
        Map<String, List<Path>> groupedFiles = new LinkedHashMap<>();

        try (var stream = Files.list(categoriesDir)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                try (var files = Files.list(dir)) {
                    files.filter(f -> f.toString().endsWith(".mdx")).forEach(f -> {
                        String filename = f.getFileName().toString();
                        Matcher m = mdxFilePattern.matcher(filename);
                        String baseSlug = m.find() ? m.group(1) : filename;
                        groupedFiles.computeIfAbsent(baseSlug, k -> new ArrayList<>()).add(f);
                    });
                } catch (IOException e) {
                    log.warn("Error listing files in {}: {}", dir, e.getMessage());
                }
            });
        } catch (IOException e) {
            errors.add("Error scanning _categories directory: " + e.getMessage());
            return List.of();
        }

        List<CategoryUpsert> categories = new ArrayList<>();
        for (Map.Entry<String, List<Path>> entry : groupedFiles.entrySet()) {
            String slug = entry.getKey();
            List<Path> files = entry.getValue();
            try {
                CategoryUpsert category = mergeAndParseCategoryFiles(slug, files);
                if (category != null) {
                    categories.add(category);
                }
            } catch (Exception e) {
                errors.add("Error parsing category " + slug + ": " + e.getMessage());
            }
        }

        return categories;
    }

    private CategoryUpsert mergeAndParseCategoryFiles(String slug, List<Path> files) throws IOException {
        Map<String, CategoryTranslationData> translations = new LinkedHashMap<>();
        List<CategoryImageRow> images = new ArrayList<>();
        String defaultLocale = "en";

        for (Path file : files) {
            String filename = file.getFileName().toString();
            String locale = extractLocale(filename);

            List<String> lines = Files.readAllLines(file);
            if (lines.isEmpty() || !"---".equals(lines.get(0).trim())) continue;
            int end = -1;
            for (int i = 1; i < lines.size(); i++) {
                if ("---".equals(lines.get(i).trim())) { end = i; break; }
            }
            if (end < 0) continue;

            String yamlBlock = String.join("\n", lines.subList(1, end));
            Map<String, Object> fm = new Yaml().load(yamlBlock);
            if (fm == null || fm.isEmpty()) continue;

            String name = str(fm, "name");
            String description = str(fm, "description");
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) fm.get("tags");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> imagesList = (List<Map<String, Object>>) fm.get("images");

            String translationLocale = locale != null ? locale : defaultLocale;
            if (name != null && !name.isBlank()) {
                translations.put(translationLocale, new CategoryTranslationData(name, description, tags));
            }

            if (images.isEmpty() && imagesList != null && !imagesList.isEmpty()) {
                images = parseCategoryImages(imagesList, file.getParent());
            }
        }

        if (translations.isEmpty()) {
            return null;
        }

        return new CategoryUpsert(slug, defaultLocale, translations, images);
    }

    private String extractLocale(String filename) {
        Pattern p = Pattern.compile("^.+\\.([a-z]{2}(?:-[A-Z]{2})?)\\.mdx$");
        Matcher m = p.matcher(filename);
        return m.find() ? m.group(1) : null;
    }

    private List<CategoryImageRow> parseCategoryImages(List<Map<String, Object>> imagesList, Path categoryDir) throws IOException {
        Path root = Paths.get(memesRoot);
        List<CategoryImageRow> images = new ArrayList<>();
        int position = 0;

        for (Object imageObj : imagesList) {
            @SuppressWarnings("unchecked")
            Map<String, Object> img = (Map<String, Object>) imageObj;
            String relativePath = (String) img.get("path");
            String imageType = (String) img.get("image_type");

            if (relativePath == null || imageType == null) continue;
            if (!imageType.matches("^(icon|banner|thumbnail)$")) continue;

            Path resolvedPath = categoryDir.resolve(relativePath.replace("./", ""));
            if (!Files.exists(resolvedPath)) continue;

            long bytes = Files.size(resolvedPath);
            String mimeType = Files.probeContentType(resolvedPath);

            Integer width = intOrNull(img.get("width"));
            Integer height = intOrNull(img.get("height"));
            Boolean isPrimary = (Boolean) img.getOrDefault("is_primary", position == 0);

            String dbPath = root.relativize(resolvedPath).toString();

            images.add(new CategoryImageRow(
                0, 0, dbPath, width, height, bytes, mimeType, imageType, position, isPrimary
            ));
            position++;
        }

        return images;
    }

    // ===== Orphan category purge ==============================================

    private void purgeOrphanCategories(List<String> errors) {
        Path root = Paths.get(memesRoot);
        if (!Files.isDirectory(root)) {
            errors.add("Cannot purge: memes root not found: " + root.toAbsolutePath());
            return;
        }

        Set<String> existingDirs;
        try (var stream = Files.list(root)) {
            existingDirs = stream
                .filter(Files::isDirectory)
                .map(p -> p.getFileName().toString())
                .filter(name -> !name.startsWith("_"))
                .collect(Collectors.toSet());
        } catch (IOException e) {
            errors.add("Failed to enumerate category directories for purge: " + e.getMessage());
            return;
        }

        List<Map<String, Object>> dbCategories = memeWriteMapper.selectAllCategoryIdsAndSlugs();
        List<String> purgedSlugs = new ArrayList<>();
        for (Map<String, Object> cat : dbCategories) {
            String slug = (String) cat.get("slug");
            if (!existingDirs.contains(slug)) {
                long orphanId = ((Number) cat.get("id")).longValue();
                memeWriteMapper.deleteMemesByCategoryId(orphanId);
                memeWriteMapper.deleteCategory(orphanId);
                purgedSlugs.add(slug);
            }
        }

        if (purgedSlugs.isEmpty()) {
            log.debug("Purge: no orphan categories found");
            return;
        }

        log.info("Purge complete: {} categories purged: {}", purgedSlugs.size(), purgedSlugs);
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
            .sourceUrl(sanitizeUrl(req.getSourceUrl(), null, "source_url"))
            .postUrl(sanitizeUrl(req.getPostUrl(), null, "post_url"))
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

    // ===== Locale helpers =====================================================

    static Optional<String> extractLocaleFromFilename(String filename) {
        Matcher m = LOCALE_MDX_PATTERN.matcher(filename);
        return m.matches() ? Optional.of(m.group(2)) : Optional.empty();
    }

    static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) return DEFAULT_LOCALE;
        String lang = locale.split("[_\\-]", 2)[0].toLowerCase();
        return ALLOWED_LOCALES.contains(lang) ? lang : DEFAULT_LOCALE;
    }

    // ===== Helpers ============================================================

    private String deriveDefaultImagePath(String category, String slug, Path mdxPath) {
        return Optional.ofNullable(mdxPath)
            .map(p -> {
                String name = p.getFileName().toString();
                Matcher lm = LOCALE_MDX_PATTERN.matcher(name);
                String base = lm.matches() ? lm.group(1)
                    : (name.endsWith(".mdx") ? name.substring(0, name.length() - 4) : slug);
                return "memes/" + category + "/" + base + ".jpg";
            })
            .orElseGet(() -> "memes/" + category + "/" + slug + ".jpg");
    }

    private String normalizeImagePath(String raw, String category, Path mdxPath, String slug) {
        if (raw == null || raw.isBlank()) {
            return deriveDefaultImagePath(category, slug, mdxPath);
        }
        if (raw.startsWith("./")) {
            return "memes/" + category + "/" + raw.substring(2);
        }
        return raw;
    }

    private String sanitizeUrl(String raw, Path mdxPath, String field) {
        if (raw == null || raw.isBlank()) return null;
        if (URL_PATTERN.matcher(raw).matches() && raw.length() <= 2048) return raw;
        log.warn("Dropping invalid {} '{}' in {}", field, raw, mdxPath);
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
