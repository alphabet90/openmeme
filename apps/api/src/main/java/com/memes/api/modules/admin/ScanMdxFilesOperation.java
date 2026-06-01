package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.modules.admin.MemeUpsert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanMdxFilesOperation implements Operation<Void, List<MemeUpsert>> {

    @Value("${memes.root}")
    private String memesRoot;

    private final ParseMdxFrontmatterOperation parseMdxFrontmatterOperation;

    @Override
    public List<MemeUpsert> execute(Void input) {
        List<String> errors = new ArrayList<>();
        List<MemeUpsert> result = scanMdxFiles(errors);
        if (!errors.isEmpty()) {
            errors.forEach(e -> log.warn("MDX scan error: {}", e));
        }
        return result;
    }

    public List<MemeUpsert> scanMdxFiles(List<String> errors) {
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
                    Path baseKey = parseMdxFrontmatterOperation.toBaseKey(p);
                    groups.computeIfAbsent(baseKey, k -> new ArrayList<>()).add(p);
                });
        } catch (IOException e) {
            errors.add("Error walking memes directory: " + e.getMessage());
            return Collections.emptyList();
        }

        List<MemeUpsert> upserts = new ArrayList<>();
        for (Map.Entry<Path, List<Path>> entry : groups.entrySet()) {
            try {
                parseMdxFrontmatterOperation.parseMdxGroup(entry.getKey(), entry.getValue()).ifPresent(upserts::add);
            } catch (Exception e) {
                String msg = "Failed to parse " + entry.getKey() + ": " + e.getMessage();
                log.warn(msg);
                errors.add(msg);
            }
        }
        return upserts;
    }
}
