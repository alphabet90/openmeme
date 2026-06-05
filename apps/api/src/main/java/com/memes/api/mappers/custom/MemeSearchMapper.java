package com.memes.api.mappers.custom;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    List<Map<String, Object>> selectMemesFlat(@Param("locale") String locale,
                                              @Param("category") String category,
                                              @Param("sort") String sort,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

    int countMemesFlat(@Param("locale") String locale,
                       @Param("category") String category);

    List<Map<String, Object>> searchMemes(@Param("query") String query,
                                           @Param("locale") String locale,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    Map<String, Object> selectMemeDetail(@Param("category") String category,
                                          @Param("slug") String slug);

    List<Map<String, Object>> selectMemeDetailsBatch(@Param("entries") List<Map<String, String>> entries);
}
