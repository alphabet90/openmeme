package com.memes.api.mappers.custom;

import com.memes.api.models.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MemeSearchMapper {
    StatsSnapshot selectStatsSnapshot();
    int countCategories();
    List<CategoryDetail> selectCategories(@Param("locale") String locale, @Param("offset") int offset, @Param("limit") int limit);
    List<CategoryImage> selectCategoryImages(@Param("categoryId") long categoryId);
    List<MemeListItem> selectMemesFlat(@Param("locale") String locale, @Param("offset") int offset, @Param("limit") int limit,
                                       @Param("category") String category, @Param("sort") String sort);
    int countMemes(@Param("locale") String locale, @Param("category") String category);
    List<MemeDetail> selectMemeDetail(@Param("category") String category, @Param("slug") String slug);
    List<SearchHit> searchMemes(@Param("query") String query, @Param("locale") String locale,
                                @Param("limit") int limit, @Param("offset") int offset);
    List<Map<String, Object>> findAllCategoryIdsAndSlugs();

    int upsertSubreddit(Subreddit subreddit);
    int upsertAuthor(Author author);
    int upsertTag(Tag tag);
    int upsertMemeTranslation(@Param("memeId") long memeId, @Param("locale") String locale,
                              @Param("title") String title, @Param("description") String description);
    int deleteMemeImages(@Param("memeId") long memeId);
    int insertMemeImage(@Param("memeId") long memeId, @Param("path") String path,
                        @Param("width") Integer width, @Param("height") Integer height,
                        @Param("bytes") Long bytes, @Param("mimeType") String mimeType,
                        @Param("position") int position, @Param("isPrimary") boolean isPrimary);
    int deleteMemeTags(@Param("memeId") long memeId);
    int insertMemeTag(@Param("memeId") long memeId, @Param("tagId") long tagId);
    int upsertCategoryTranslation(@Param("categoryId") long categoryId, @Param("locale") String locale,
                                  @Param("name") String name, @Param("description") String description);
    int deleteCategoryImages(@Param("categoryId") long categoryId);
    int insertCategoryImage(@Param("categoryId") long categoryId, @Param("path") String path,
                            @Param("width") Integer width, @Param("height") Integer height,
                            @Param("bytes") Long bytes, @Param("mimeType") String mimeType,
                            @Param("imageType") String imageType, @Param("position") int position,
                            @Param("isPrimary") boolean isPrimary);
    void refreshStats();
    int purgeCategory(@Param("categoryId") long categoryId);
    int countCategoryMemes(@Param("categoryId") long categoryId);
}
