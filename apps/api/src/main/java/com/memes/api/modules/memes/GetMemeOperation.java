package com.memes.api.modules.memes;

import com.memes.api.common.dto.GetMemeInput;
import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.LocaleCode;
import com.memes.api.generated.model.Meme;
import com.memes.api.generated.model.MemeImage;
import com.memes.api.generated.model.MemeTranslation;
import com.memes.api.mappers.custom.MemeSearchMapper;
import com.memes.api.models.MemeDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetMemeOperation implements Operation<GetMemeInput, Optional<Meme>> {

    private final MemeSearchMapper memeSearchMapper;

    @Value("${memes.cdn-url:}")
    private String cdnUrl;

    @Override
    public Optional<Meme> execute(GetMemeInput input) {
        var details = memeSearchMapper.selectMemeDetail(input.getCategory(), input.getSlug());
        if (details.isEmpty()) return Optional.empty();
        return Optional.of(toMeme(details.getFirst(), input.getLocale()));
    }

    private Meme toMeme(MemeDetail r, String locale) {
        Meme m = new Meme();
        m.setSlug(r.getSlug());
        m.setCategory(r.getCategorySlug());
        m.setDefaultLocale(toLocaleCode(r.getDefaultLocale()));
        m.setAuthor(r.getAuthorUsername());
        m.setSubreddit(r.getSubredditName());
        m.setScore(r.getScore());
        Optional.ofNullable(r.getCreatedAt()).ifPresent(m::setCreatedAt);
        m.setSourceUrl(r.getSourceUrl());
        m.setPostUrl(r.getPostUrl());
        m.setTranslations(r.getTranslations().stream()
            .filter(t -> locale.equals(t.getLocale()))
            .map(this::toTranslation).collect(Collectors.toList()));
        m.setImages(r.getImages().stream().map(this::toImage).collect(Collectors.toList()));
        m.setTags(r.getTagSlugs());
        return m;
    }

    private MemeTranslation toTranslation(com.memes.api.models.MemeTranslation t) {
        MemeTranslation out = new MemeTranslation();
        out.setLocale(toLocaleCode(t.getLocale()));
        out.setTitle(t.getTitle());
        out.setDescription(t.getDescription());
        return out;
    }

    private MemeImage toImage(com.memes.api.models.MemeImage img) {
        MemeImage out = new MemeImage();
        out.setPath(resolveImageUrl(img.getPath()));
        out.setWidth(img.getWidth());
        out.setHeight(img.getHeight());
        out.setBytes(img.getBytes());
        out.setMimeType(img.getMimeType());
        out.setPosition(img.getPosition());
        out.setIsPrimary(img.getIsPrimary());
        return out;
    }

    private static LocaleCode toLocaleCode(String value) {
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
