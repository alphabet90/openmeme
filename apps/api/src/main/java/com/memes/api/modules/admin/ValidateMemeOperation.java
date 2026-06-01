package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.MemeIndexRequest;
import com.memes.api.repository.MemeTranslationRow;
import com.memes.api.repository.MemeUpsert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ValidateMemeOperation implements Operation<MemeIndexRequest, MemeUpsert> {

    static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    static final Pattern URL_PATTERN = Pattern.compile("^(https://|/).+", Pattern.CASE_INSENSITIVE);

    @Override
    public MemeUpsert execute(MemeIndexRequest req) {
        String category = Optional.ofNullable(req.getCategory()).orElse("");
        String slug = Optional.ofNullable(req.getSlug()).orElse("");
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new IllegalArgumentException("slug violates slug domain: " + slug);
        }
        if (!SLUG_PATTERN.matcher(category).matches()) {
            throw new IllegalArgumentException("category violates slug domain: " + category);
        }
        String defaultLocale = Optional.ofNullable(req.getDefaultLocale())
            .orElse(com.memes.api.generated.model.LocaleCode.EN).getValue();

        var translations = Optional.ofNullable(req.getTranslations())
            .orElse(List.of())
            .stream()
            .map(t -> MemeTranslationRow.builder()
                .locale(t.getLocale().getValue())
                .title(t.getTitle())
                .description(t.getDescription())
                .build())
            .toList();
        if (translations.isEmpty()) {
            throw new IllegalArgumentException("translations[] is required");
        }
        boolean hasDefault = translations.stream().anyMatch(t -> defaultLocale.equals(t.locale()));
        if (!hasDefault) {
            throw new IllegalArgumentException(
                "no translation for default_locale '" + defaultLocale + "'");
        }

        var images = Optional.ofNullable(req.getImages())
            .filter(l -> !l.isEmpty())
            .map(list -> list.stream().map(img -> com.memes.api.repository.MemeImageRow.builder()
                .path(img.getPath())
                .width(img.getWidth())
                .height(img.getHeight())
                .bytes(img.getBytes())
                .mimeType(img.getMimeType())
                .position(Optional.ofNullable(img.getPosition()).orElse(0))
                .isPrimary(Boolean.TRUE.equals(img.getIsPrimary()))
                .build())
                .toList())
            .orElseGet(() -> List.of(com.memes.api.repository.MemeImageRow.builder()
                .path("memes/" + category + "/" + slug + ".jpg")
                .position(0)
                .isPrimary(true)
                .build()));

        return MemeUpsert.builder()
            .slug(slug)
            .categorySlug(category)
            .defaultLocale(defaultLocale)
            .subredditName(req.getSubreddit())
            .authorUsername(req.getAuthor())
            .score(Optional.ofNullable(req.getScore()).orElse(0))
            .createdAt(req.getCreatedAt())
            .sourceUrl(req.getSourceUrl())
            .postUrl(req.getPostUrl())
            .translations(translations)
            .images(images)
            .tagSlugs(Optional.ofNullable(req.getTags()).orElse(List.of()))
            .build();
    }
}
