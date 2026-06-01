package com.memes.api.mappers;

import com.memes.api.models.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CategoryMapper {

    @Select("SELECT id, slug FROM categories WHERE id = #{id}")
    Optional<Category> selectById(Long id);

    @Select("SELECT id, slug FROM categories WHERE slug = #{slug}")
    Optional<Category> selectBySlug(String slug);

    @Select("SELECT id, slug FROM categories")
    List<Category> selectAll();

    @Insert("INSERT INTO categories (slug) VALUES (#{slug}) "
          + "ON CONFLICT (slug) DO UPDATE SET slug = EXCLUDED.slug")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void upsert(Category category);

    @Insert("INSERT INTO categories (slug) VALUES (#{slug}) "
          + "ON CONFLICT (slug) DO UPDATE SET slug = EXCLUDED.slug RETURNING id")
    long upsertReturningId(String slug);

    @Select("SELECT id, slug FROM categories WHERE id IN (SELECT category_id FROM memes WHERE deleted_at IS NULL)")
    List<Category> selectActiveCategories();
}
