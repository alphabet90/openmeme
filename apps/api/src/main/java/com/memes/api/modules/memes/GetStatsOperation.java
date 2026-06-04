package com.memes.api.modules.memes;

import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.Stats;
import com.memes.api.mappers.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import com.memes.api.common.constants.CacheNames;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetStatsOperation implements Operation<Void, Stats> {

    private final StatsMapper statsMapper;

    @Override
    @Cacheable(value = CacheNames.STATS)
    public Stats execute(Void input) {
        Stats stats = new Stats();
        statsMapper.selectStatsSnapshot().ifPresent(row -> {
            stats.setTotalMemes(Optional.ofNullable(row.getTotalMemes()).map(Long::intValue).orElse(0));
            stats.setTotalCategories(Optional.ofNullable(row.getTotalCategories()).map(Long::intValue).orElse(0));
            stats.setTotalSubreddits(Optional.ofNullable(row.getTotalSubreddits()).map(Long::intValue).orElse(0));
            Optional.ofNullable(row.getTopCategory()).ifPresent(stats::setTopCategory);
            Optional.ofNullable(row.getIndexedAt()).ifPresent(stats::setIndexedAt);
        });
        return stats;
    }
}
