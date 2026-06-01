package com.memes.api.mappers.custom;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Custom mapper for complex meme queries that use PostgreSQL functions
 * and JSON aggregations. These queries are too complex for generated
 * MyBatis mappers and are hand-written here.
 */
@Mapper
public interface MemeSearchMapper {

    @Select("SELECT id, slug, score, created_at, category_slug, author_username, "
          + "title, description, image_path, tags "
          + "FROM list_memes_flat(#{locale}::locale_code) "
          + "WHERE 1=1 "
          + "<script>"
          + "<if test='category != null and !category.isEmpty()'> AND category_slug = #{category}</if>"
          + "</script> "
          + "ORDER BY "
          + "<choose>"
          + "<when test=\"sort == 'title'\">title ASC NULLS LAST, id DESC</when>"
          + "<when test=\"sort == 'created_at'\">created_at DESC NULLS LAST, id DESC</when>"
          + "<otherwise>score DESC, id DESC</otherwise>"
          + "</choose> "
          + "LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectMemesFlat(@Param("locale") String locale,
                                              @Param("category") String category,
                                              @Param("sort") String sort,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM list_memes_flat(#{locale}::locale_code) "
          + "WHERE 1=1 "
          + "<script>"
          + "<if test='category != null and !category.isEmpty()'> AND category_slug = #{category}</if>"
          + "</script>")
    int countMemesFlat(@Param("locale") String locale,
                       @Param("category") String category);

    @Select("SELECT meme_id, slug, category, title, description, score, rank, total_count "
          + "FROM search_memes(#{query}, #{locale}::locale_code, #{limit}, #{offset})")
    List<Map<String, Object>> searchMemes(@Param("query") String query,
                                           @Param("locale") String locale,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    Map<String, Object> selectMemeDetail(@Param("category") String category,
                                          @Param("slug") String slug);

    List<Map<String, Object>> selectMemeDetailsBatch(@Param("entries") List<Map<String, String>> entries);
}
