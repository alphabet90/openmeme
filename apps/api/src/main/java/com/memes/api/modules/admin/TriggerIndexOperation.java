package com.memes.api.modules.admin;

import com.memes.api.common.dto.IndexerInput;
import com.memes.api.common.operation.Operation;
import com.memes.api.config.RedisConfig;
import com.memes.api.generated.model.MemeIndexRequest;
import com.memes.api.mappers.custom.MemeSearchMapper;
import com.memes.api.repository.CategoryImageRow;
import com.memes.api.repository.MemeUpsert;
import com.memes.api.common.dto.IndexResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class TriggerIndexOperation implements Operation<IndexerInput, Void> {

    private final ScanMdxFilesOperation scanMdxFilesOperation;
    private final UpsertMemeOperation upsertMemeOperation;
    private final UpsertCategoryOperation upsertCategoryOperation;
    private final PurgeOrphanCategoriesOperation purgeOrphanCategoriesOperation;
    private final InvalidateCachesOperation invalidateCachesOperation;
    private final ValidateMemeOperation validateMemeOperation;
    private final MemeSearchMapper memeSearchMapper;
    private final CacheManager cacheManager;

    @Value("${memes.root}")
    private String memesRoot;

    @Override
    public Void execute(IndexerInput input) {
        IndexResult result = Optional.ofNullable(input.getSingleMemeRequest())
            .filter(r -> r.getSlug() != null && !r.getSlug().isBlank())
            .map(this::indexSingle)
            .orElseGet(() -> reindex(input.isIndexMemes(), input.isIndexCategories()));
        log.info("Reindex done: indexed={} durationMs={} errors={}",
            result.indexed(), result.durationMs(), result.errors().size());
        return null;
    }

    private IndexResult indexSingle(MemeIndexRequest req) {
        long start = System.currentTimeMillis();
        MemeUpsert upsert = validateMemeOperation.execute(req);
        upsertMemeOperation.execute(upsert);
        memeSearchMapper.refreshStats();
        invalidateCachesOperation.execute(null);
        long duration = System.currentTimeMillis() - start;
        log.info("Single meme indexed: {}/{} in {}ms", upsert.categorySlug(), upsert.slug(), duration);
        return new IndexResult(1, duration, List.of());
    }

    private IndexResult reindex(boolean indexMemes, boolean indexCategories) {
        long start = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        int categoryCount = 0;
        int memeCount = 0;

        if (indexCategories) {
            List<CategoryUpsert> categories = scanCategoryMdxFiles(errors);
            for (CategoryUpsert cat : categories) {
                upsertCategoryOperation.execute(cat);
            }
            categoryCount = categories.size();
        }

        if (indexMemes) {
            List<MemeUpsert> memes = scanMdxFilesOperation.execute(null);
            for (MemeUpsert m : memes) {
                try {
                    upsertMemeOperation.execute(m);
                    memeCount++;
                } catch (RuntimeException e) {
                    log.warn("Failed to upsert meme {}/{}: {}", m.categorySlug(), m.slug(), e.getMessage());
                    errors.add("Failed to upsert meme " + m.categorySlug() + "/" + m.slug() + ": " + e.getMessage());
                }
            }
        }

        if (indexCategories) {
            PurgeOrphanCategoriesOperation.PurgeResult purgeResult = purgeOrphanCategoriesOperation.execute(null);
            if (purgeResult.deletedCategories() > 0) {
                log.info("Purge: {} categories, {} memes", purgeResult.deletedCategories(), purgeResult.deletedMemes());
            }
        }

        memeSearchMapper.refreshStats();
        invalidateCachesOperation.execute(null);
        long duration = System.currentTimeMillis() - start;
        log.info("Reindex complete: {} categories, {} memes indexed in {}ms ({} errors)",
            categoryCount, memeCount, duration, errors.size());
        return new IndexResult(memeCount, duration, errors);
    }

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
            try {
                CategoryUpsert cat = mergeAndParseCategoryFiles(entry.getKey(), entry.getValue());
                if (cat != null) categories.add(cat);
            } catch (Exception e) {
                errors.add("Error parsing category " + entry.getKey() + ": " + e.getMessage());
            }
        }
        return categories;
    }

    @SuppressWarnings("unchecked")
    private CategoryUpsert mergeAndParseCategoryFiles(String slug, List<Path> files) throws IOException {
        Map<String, CategoryTranslationData> translations = new LinkedHashMap<>();
        String defaultLocale = "en";

        for (Path file : files) {
            String filename = file.getFileName().toString();
            String locale = extractCategoryLocale(filename);

            List<String> lines = Files.readAllLines(file);
            if (lines.isEmpty() || !"---".equals(lines.getFirst().trim())) continue;
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

            String translationLocale = locale != null ? locale : defaultLocale;
            if (name != null && !name.isBlank()) {
                translations.put(translationLocale,
                    new CategoryTranslationData(name, description, List.of()));
            }
        }

        if (translations.isEmpty()) return null;
        return new CategoryUpsert(slug, defaultLocale, translations, List.of());
    }

    private String extractCategoryLocale(String filename) {
        Pattern p = Pattern.compile("^.+\\.([a-z]{2}(?:-[A-Z]{2})?)\\.mdx$");
        Matcher m = p.matcher(filename);
        return m.find() ? m.group(1) : null;
    }

    private String str(Map<String, Object> fm, String key) {
        return Optional.ofNullable(fm.get(key)).map(Object::toString).orElse(null);
    }
}
