package com.memes.api.modules.memes;

import com.memes.api.common.dto.GetStatsInput;
import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.Stats;
import com.memes.api.mappers.custom.MemeSearchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetStatsOperation implements Operation<GetStatsInput, Stats> {

    private final MemeSearchMapper memeSearchMapper;

    @Override
    public Stats execute(GetStatsInput input) {
        Stats stats = new Stats();
        memeSearchMapper.selectStatsSnapshot().ifPresent(row -> {
            stats.setTotalMemes((int) row.getTotalMemes());
            stats.setTotalCategories((int) row.getTotalCategories());
            stats.setTotalSubreddits((int) row.getTotalSubreddits());
            Optional.ofNullable(row.getTopCategory()).ifPresent(stats::setTopCategory);
            Optional.ofNullable(row.getIndexedAt()).ifPresent(stats::setIndexedAt);
        });
        return stats;
    }
}
