package com.memes.api.mappers;

import com.memes.api.models.StatsSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface StatsMapper {

    @Select("SELECT total_memes, total_categories, total_subreddits, top_category, indexed_at "
          + "FROM stats_snapshot WHERE singleton = 1")
    Optional<StatsSnapshot> selectStatsSnapshot();
}
