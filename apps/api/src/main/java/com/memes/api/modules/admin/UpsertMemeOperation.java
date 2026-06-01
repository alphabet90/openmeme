package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.mappers.CategoryMapper;
import com.memes.api.mappers.MemeMapper;
import com.memes.api.mappers.custom.MemeSearchMapper;
import com.memes.api.models.Category;
import com.memes.api.models.Meme;
import com.memes.api.modules.admin.MemeImageRow;
import com.memes.api.modules.admin.MemeTranslationRow;
import com.memes.api.modules.admin.MemeUpsert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpsertMemeOperation implements Operation<MemeUpsert, Long> {

    private final CategoryMapper categoryMapper;
    private final MemeMapper memeMapper;
    private final MemeSearchMapper memeSearchMapper;

    @Override
    @Transactional
    public Long execute(MemeUpsert u) {
        long categoryId = upsertCategory(u.categorySlug());

        long subredditId = 0;
        Optional.ofNullable(u.subredditName()).filter(s -> !s.isBlank()).ifPresent(name -> {
            var sub = new com.memes.api.models.Subreddit();
            sub.setName(name);
            memeSearchMapper.upsertSubreddit(sub);
            subredditId = Optional.ofNullable(sub.getId()).orElse(0L);
        });

        long authorId = 0;
        Optional.ofNullable(u.authorUsername()).filter(a -> !a.isBlank()).ifPresent(username -> {
            var author = new com.memes.api.models.Author();
            author.setUsername(username);
            memeSearchMapper.upsertAuthor(author);
            authorId = Optional.ofNullable(author.getId()).orElse(0L);
        });

        Timestamp createdAt = Optional.ofNullable(u.createdAt())
            .map(o -> Timestamp.from(o.toInstant()))
            .orElse(null);

        Meme meme = new Meme();
        meme.setCategoryId(categoryId);
        meme.setSlug(u.slug());
        meme.setSubredditId(subredditId > 0 ? subredditId : null);
        meme.setAuthorId(authorId > 0 ? authorId : null);
        meme.setDefaultLocale(u.defaultLocale());
        meme.setScore(u.score());
        meme.setSourceUrl(u.sourceUrl());
        meme.setPostUrl(u.postUrl());
        meme.setCreatedAt(createdAt != null
            ? java.time.OffsetDateTime.ofInstant(createdAt.toInstant(), ZoneOffset.UTC) : null);

        memeMapper.upsert(meme);
        long memeId = Optional.ofNullable(meme.getId())
            .orElseThrow(() -> new IllegalStateException("Upsert returned null meme id for " + u.slug()));

        upsertTranslations(memeId, u.translations());
        replaceImages(memeId, u.images());
        replaceTags(memeId, u.tagSlugs());

        return memeId;
    }

    private long upsertCategory(String slug) {
        Category cat = new Category();
        cat.setSlug(slug);
        categoryMapper.upsert(cat);
        return Optional.ofNullable(cat.getId())
            .orElseThrow(() -> new IllegalStateException("Category upsert returned null id for " + slug));
    }

    private void upsertTranslations(long memeId, java.util.List<MemeTranslationRow> translations) {
        if (translations == null || translations.isEmpty()) return;
        for (var t : translations) {
            memeSearchMapper.upsertMemeTranslation(memeId, t.locale(), t.title(), t.description());
        }
    }

    private void replaceImages(long memeId, java.util.List<MemeImageRow> images) {
        memeSearchMapper.deleteMemeImages(memeId);
        if (images == null || images.isEmpty()) return;
        for (var img : images) {
            memeSearchMapper.insertMemeImage(memeId, img.path(), img.width(), img.height(),
                img.bytes(), img.mimeType(), img.position(), img.isPrimary());
        }
    }

    private void replaceTags(long memeId, java.util.List<String> tagSlugs) {
        memeSearchMapper.deleteMemeTags(memeId);
        if (tagSlugs == null || tagSlugs.isEmpty()) return;
        for (String slug : tagSlugs) {
            if (slug == null || slug.isBlank()) continue;
            var tag = new com.memes.api.models.Tag();
            tag.setSlug(slug);
            memeSearchMapper.upsertTag(tag);
            long tagId = Optional.ofNullable(tag.getId()).orElse(0L);
            if (tagId > 0) {
                memeSearchMapper.insertMemeTag(memeId, tagId);
            }
        }
    }
}
