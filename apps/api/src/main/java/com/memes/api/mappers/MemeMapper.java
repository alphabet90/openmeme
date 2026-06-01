package com.memes.api.mappers;

import com.memes.api.models.Meme;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.Optional;

@Mapper
public interface MemeMapper {

    @Select("SELECT m.id, m.category_id, m.slug, m.subreddit_id, m.author_id, "
          + "m.default_locale, m.score, m.source_url, m.post_url, "
          + "m.created_at, m.indexed_at, m.deleted_at "
          + "FROM memes m WHERE m.id = #{id}")
    Optional<Meme> selectById(Long id);

    @Insert("INSERT INTO memes (category_id, slug, subreddit_id, author_id, default_locale, "
          + "score, source_url, post_url, created_at) "
          + "VALUES (#{categoryId}, #{slug}, #{subredditId}, #{authorId}, "
          + "#{defaultLocale}::locale_code, #{score}, #{sourceUrl}, #{postUrl}, #{createdAt}) "
          + "ON CONFLICT (category_id, slug) DO UPDATE SET "
          + "subreddit_id = EXCLUDED.subreddit_id, author_id = EXCLUDED.author_id, "
          + "default_locale = EXCLUDED.default_locale, score = EXCLUDED.score, "
          + "source_url = EXCLUDED.source_url, post_url = EXCLUDED.post_url, "
          + "created_at = EXCLUDED.created_at, indexed_at = now(), deleted_at = NULL")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void upsert(Meme meme);

    @Update("UPDATE memes SET deleted_at = NOW() WHERE category_id = #{categoryId}")
    int softDeleteByCategoryId(Long categoryId);
}
