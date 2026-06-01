package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.mappers.custom.MemeSearchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurgeOrphanCategoriesOperation implements Operation<Void, PurgeOrphanCategoriesOperation.PurgeResult> {

    @Value("${memes.root}")
    private String memesRoot;

    private final MemeSearchMapper memeSearchMapper;

    public record PurgeResult(int deletedCategories, int deletedMemes, List<String> purgedSlugs) {}

    @Override
    public PurgeResult execute(Void input) {
        List<String> errors = new ArrayList<>();
        PurgeResult result = purgeOrphanCategories(errors);
        if (!errors.isEmpty()) {
            errors.forEach(e -> log.warn("Purge error: {}", e));
        }
        return result;
    }

    private PurgeResult purgeOrphanCategories(List<String> errors) {
        Path root = Paths.get(memesRoot);
        if (!Files.isDirectory(root)) {
            errors.add("Cannot purge: memes root not found: " + root.toAbsolutePath());
            return new PurgeResult(0, 0, List.of());
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
            return new PurgeResult(0, 0, List.of());
        }

        List<Map<String, Object>> dbCategories = memeSearchMapper.findAllCategoryIdsAndSlugs();
        Set<Long> orphanIds = new HashSet<>();
        List<String> purgedSlugs = new ArrayList<>();
        for (Map<String, Object> cat : dbCategories) {
            String slug = (String) cat.get("slug");
            if (!existingDirs.contains(slug)) {
                orphanIds.add(((Number) cat.get("id")).longValue());
                purgedSlugs.add(slug);
            }
        }

        if (orphanIds.isEmpty()) {
            log.debug("Purge: no orphan categories found");
            return new PurgeResult(0, 0, List.of());
        }

        int deletedMemes = 0;
        for (Long id : orphanIds) {
            int count = memeSearchMapper.countCategoryMemes(id);
            memeSearchMapper.purgeCategory(id);
            deletedMemes += count;
        }

        log.info("Purge complete: {} orphan categories, {} memes deleted: {}",
            orphanIds.size(), deletedMemes, purgedSlugs);
        return new PurgeResult(orphanIds.size(), deletedMemes, purgedSlugs);
    }
}
