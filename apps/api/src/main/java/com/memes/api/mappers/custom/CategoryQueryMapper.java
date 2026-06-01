package com.memes.api.mappers.custom;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CategoryQueryMapper {

    @Select("SELECT cc.category_id, cc.category, cc.count, cc.top_score, "
          + "ct.locale, ct.name, ct.description "
          + "FROM category_counts cc "
          + "INNER JOIN category_translations ct "
          + "ON ct.category_id = cc.category_id AND ct.locale = #{locale}::locale_code "
          + "ORDER BY cc.count DESC, cc.category ASC "
          + "LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> selectCategoriesWithTranslations(@Param("locale") String locale,
                                                                @Param("limit") int limit,
                                                                @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM category_counts")
    int countCategories();

    @Select("SELECT id, category_id, path, width, height, bytes, mime_type, image_type, position, is_primary "
          + "FROM category_images WHERE category_id = #{categoryId} ORDER BY position ASC")
    List<Map<String, Object>> selectCategoryImages(long categoryId);

    @Select("SELECT id, category_id, path, width, height, bytes, mime_type, image_type, position, is_primary "
          + "FROM category_images WHERE category_id = #{categoryId} AND image_type = #{imageType} ORDER BY position ASC")
    List<Map<String, Object>> selectCategoryImagesByType(@Param("categoryId") long categoryId,
                                                           @Param("imageType") String imageType);

    @Select("<script>"
          + "SELECT id, category_id, path, width, height, bytes, mime_type, image_type, position, is_primary "
          + "FROM category_images WHERE category_id IN "
          + "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> "
          + "ORDER BY position ASC"
          + "</script>")
    List<Map<String, Object>> selectCategoryImagesByBatch(@Param("ids") List<Long> ids);
}
