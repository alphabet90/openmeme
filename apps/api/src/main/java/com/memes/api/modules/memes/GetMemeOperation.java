package com.memes.api.modules.memes;

import com.memes.api.common.dto.GetMemeInput;
import com.memes.api.common.constants.CacheNames;
import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.LocaleCode;
import com.memes.api.generated.model.Meme;
import com.memes.api.generated.model.MemeImage;
import com.memes.api.generated.model.MemeTranslation;
import com.memes.api.mappers.custom.MemeSearchMapper;
import com.memes.api.repository.JsonAggregates;
import com.memes.api.repository.MemeImageRow;
import com.memes.api.repository.MemeTranslationRow;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetMemeOperation implements Operation<GetMemeInput, Optional<Meme>> {

    private final MemeSearchMapper memeSearchMapper;

    @Value("${memes.cdn-url:}")
    private String cdnUrl;

    @Override
    @Cacheable(value = CacheNames.MEME, key = "#input.category + '/' + #input.slug + '-' + #input.locale")
    public Optional<Meme> execute(GetMemeInput input) {
        Map<String, Object> detail = memeSearchMapper.selectMemeDetail(input.category(), input.slug());
        if (detail == null || detail.isEmpty()) return Optional.empty();
        return Optional.of(toMeme(detail, input.locale()));
    }

    @SuppressWarnings("unchecked")
    private Meme toMeme(Map<String, Object> r, String locale) {
        Meme m = new Meme();
        m.setSlug((String) r.get("slug"));
        m.setCategory((String) r.get("category_slug"));
        m.setDefaultLocale(toLocaleCode((String) r.get("default_locale")));
        m.setAuthor((String) r.get("author_username"));
        m.setSubreddit((String) r.get("subreddit_name"));
        m.setScore(r.get("score") instanceof Number n ? n.intValue() : 0);
        Optional.ofNullable((OffsetDateTime) r.get("created_at")).ifPresent(m::setCreatedAt);
        m.setSourceUrl((String) r.get("source_url"));
        m.setPostUrl((String) r.get("post_url"));

        String translationsJson = (String) r.get("translations_json");
        var allTranslations = translationsJson != null
            ? JsonAggregates.parseTranslations(translationsJson)
            : List.<MemeTranslationRow>of();
        m.setTranslations(allTranslations.stream()
            .filter(t -> locale.equals(t.locale()))
            .map(t -> {
                MemeTranslation out = new MemeTranslation();
                out.setLocale(toLocaleCode(t.locale()));
                out.setTitle(t.title());
                out.setDescription(t.description());
                return out;
            })
            .toList());

        String imagesJson = (String) r.get("images_json");
        var allImages = imagesJson != null
            ? JsonAggregates.parseImages(imagesJson)
            : List.<MemeImageRow>of();
        m.setImages(allImages.stream().map(img -> {
            MemeImage out = new MemeImage();
            out.setPath(resolveImageUrl(img.path()));
            out.setWidth(img.width());
            out.setHeight(img.height());
            out.setBytes(img.bytes());
            out.setMimeType(img.mimeType());
            out.setPosition(img.position());
            out.setIsPrimary(img.isPrimary());
            return out;
        }).toList());

        Object tags = r.get("tag_slugs");
        if (tags instanceof String[] arr) {
            m.setTags(Arrays.asList(arr));
        } else if (tags instanceof List<?> list) {
            m.setTags((List<String>) list);
        } else {
            m.setTags(List.of());
        }
        return m;
    }

    private String resolveImageUrl(String relativePath) {
        if (!StringUtils.hasText(cdnUrl) || !StringUtils.hasText(relativePath)) {
            return relativePath;
        }
        String base = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
        String path = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return base + "/" + path;
    }

    private static LocaleCode toLocaleCode(@Nullable String value) {
        return Optional.ofNullable(value)
            .flatMap(v -> Optional.ofNullable(LocaleCode.fromValue(v)))
            .orElse(LocaleCode.EN);
    }
}
