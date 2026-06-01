package com.memes.api.mappers;

import com.memes.api.models.Meme;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface MemeMapper {
    int upsert(Meme meme);
    Optional<Meme> findBySlugAndCategory(@Param("category") String category, @Param("slug") String slug);
    Optional<Meme> findById(@Param("id") long id);
}
