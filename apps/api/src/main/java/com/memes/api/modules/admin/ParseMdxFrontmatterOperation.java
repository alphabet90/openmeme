package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.modules.admin.MemeImageRow;
import com.memes.api.modules.admin.MemeTranslationRow;
import com.memes.api.modules.admin.MemeUpsert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ParseMdxFrontmatterOperation implements Operation<Path, Optional<MemeUpsert>> {

    static final Set<String> ALLOWED_LOCALES = Set.of("en", "es", "pt", "fr", "de", "ar");
    static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    static final Pattern URL_PATTERN = Pattern.compile("^(https://|/).+", Pattern.CASE_INSENSITIVE);
    static final String DEFAULT_LOCALE = "en";
    static final Pattern LOCALE_MDX_PATTERN =
        Pattern.compile("^(.+)\\.([a-z]{2}(?:-[A-Z]{2})?)\\.mdx$");

    @Override
    public Optional<MemeUpsert> execute(Path mdxPath) {
        try {
            return parseMdx(mdxPath);
        } catch (Exception e) {
            log.warn("Failed to parse {}: {}", mdxPath, e.getMessage());
            return Optional.empty();
        }
    }

    Path toBaseKey(Path mdxPath) {
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
            if (skeleton == null) {
                skeleton = u;
            }

            for (MemeTranslationRow t : u.translations()) {
                if (seenLocales.add(t.locale())) {
                    allTranslations.add(t);
                }
            }
        }

        if (skeleton == null || allTranslations.isEmpty()) return Optional.empty();

        String defaultLocale = skeleton.defaultLocale();
        boolean hasDefault = allTranslations.stream().anyMatch(t -> defaultLocale.equals(t.locale()));
        if (!hasDefault) return Optional.empty();

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
        if (lines.isEmpty() || !"---".equals(lines.getFirst().trim())) {
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
                .map(ParseMdxFrontmatterOperation::normalizeLocale)
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

    static Optional<String> extractLocaleFromFilename(String filename) {
        Matcher m = LOCALE_MDX_PATTERN.matcher(filename);
        return m.matches() ? Optional.of(m.group(2)) : Optional.empty();
    }

    static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) return DEFAULT_LOCALE;
        String lang = locale.split("[_\\-]", 2)[0].toLowerCase();
        return ALLOWED_LOCALES.contains(lang) ? lang : DEFAULT_LOCALE;
    }

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
}
