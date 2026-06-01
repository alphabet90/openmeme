package com.memes.api.mappers;

import com.memes.api.models.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CategoryMapper {
    int upsert(Category category);
    Category findBySlug(@Param("slug") String slug);
}
