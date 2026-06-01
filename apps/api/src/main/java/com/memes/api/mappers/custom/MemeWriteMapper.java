package com.memes.api.mappers.custom;

import com.memes.api.models.Meme;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface MemeWriteMapper {

    @Insert("INSERT INTO categories (slug) VALUES (#{slug}) "
          + "ON CONFLICT (slug) DO UPDATE SET slug = EXCLUDED.slug RETURNING id")
    long upsertCategoryReturningId(String slug);

    @Insert("INSERT INTO subreddits (name) VALUES (#{name}) "
          + "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id")
    long upsertSubredditReturningId(String name);

    @Insert("INSERT INTO authors (username) VALUES (#{username}) "
          + "ON CONFLICT (username) DO UPDATE SET username = EXCLUDED.username RETURNING id")
    long upsertAuthorReturningId(String username);

    @Insert("INSERT INTO tags (slug) VALUES (#{slug}) "
          + "ON CONFLICT (slug) DO UPDATE SET slug = EXCLUDED.slug RETURNING id")
    long upsertTagReturningId(String slug);

    @Insert("INSERT INTO meme_translations (meme_id, locale, title, description) VALUES "
          + "(#{memeId}, #{locale}::locale_code, #{title}, #{description}) "
          + "ON CONFLICT (meme_id, locale) DO UPDATE SET title = EXCLUDED.title, description = EXCLUDED.description")
    void upsertMemeTranslation(@Param("memeId") long memeId,
                               @Param("locale") String locale,
                               @Param("title") String title,
                               @Param("description") String description);

    @Insert("INSERT INTO meme_images (meme_id, path, width, height, bytes, mime_type, position, is_primary) "
          + "VALUES (#{memeId}, #{path}, #{width}, #{height}, #{bytes}, #{mimeType}, #{position}, #{isPrimary})")
    void insertMemeImage(@Param("memeId") long memeId,
                         @Param("path") String path,
                         @Param("width") Integer width,
                         @Param("height") Integer height,
                         @Param("bytes") Long bytes,
                         @Param("mimeType") String mimeType,
                         @Param("position") int position,
                         @Param("isPrimary") boolean isPrimary);

    @Delete("DELETE FROM meme_images WHERE meme_id = #{memeId}")
    int deleteMemeImages(long memeId);

    @Delete("DELETE FROM meme_tags WHERE meme_id = #{memeId}")
    int deleteMemeTags(long memeId);

    @Insert("INSERT INTO meme_tags (meme_id, tag_id) VALUES (#{memeId}, #{tagId}) ON CONFLICT DO NOTHING")
    void insertMemeTag(@Param("memeId") long memeId, @Param("tagId") long tagId);

    @Select("SELECT id, slug FROM categories")
    List<Map<String, Object>> selectAllCategoryIdsAndSlugs();

    @Select("SELECT id, slug, name, description FROM categories WHERE slug = #{slug}")
    Map<String, Object> selectCategoryBySlug(String slug);

    @Update("UPDATE memes SET deleted_at = NOW() WHERE category_id = #{categoryId}")
    int softDeleteMemesByCategoryId(long categoryId);

    @Delete("DELETE FROM categories WHERE id = #{id}")
    int deleteCategory(long id);

    @Delete("DELETE FROM memes WHERE category_id = #{categoryId}")
    int deleteMemesByCategoryId(long categoryId);

    @Insert("INSERT INTO category_translations (category_id, locale, name, description) "
          + "VALUES (#{categoryId}, #{locale}::locale_code, #{name}, #{description}) "
          + "ON CONFLICT (category_id, locale) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description")
    void upsertCategoryTranslation(@Param("categoryId") long categoryId,
                                    @Param("locale") String locale,
                                    @Param("name") String name,
                                    @Param("description") String description);

    @Delete("DELETE FROM category_images WHERE category_id = #{categoryId}")
    int deleteCategoryImages(long categoryId);

    @Insert("INSERT INTO category_images (category_id, path, width, height, bytes, mime_type, image_type, position, is_primary) "
          + "VALUES (#{categoryId}, #{path}, #{width}, #{height}, #{bytes}, #{mimeType}, #{imageType}, #{position}, #{isPrimary})")
    void insertCategoryImage(@Param("categoryId") long categoryId,
                              @Param("path") String path,
                              @Param("width") Integer width,
                              @Param("height") Integer height,
                              @Param("bytes") Long bytes,
                              @Param("mimeType") String mimeType,
                              @Param("imageType") String imageType,
                              @Param("position") int position,
                              @Param("isPrimary") boolean isPrimary);

    @Update("SELECT refresh_stats()")
    void refreshStats();

    @Select("SELECT id, slug FROM categories WHERE id IN (SELECT category_id FROM memes WHERE deleted_at IS NULL)")
    List<Map<String, Object>> selectActiveCategoryIdsAndSlugs();
}
