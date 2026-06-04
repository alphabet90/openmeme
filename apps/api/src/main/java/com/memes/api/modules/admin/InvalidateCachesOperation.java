package com.memes.api.modules.admin;

import com.memes.api.common.operation.Operation;
import com.memes.api.common.constants.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvalidateCachesOperation implements Operation<Void, Void> {

    private final CacheManager cacheManager;

    @Override
    public Void execute(Void input) {
        List.of(
            CacheNames.STATS, CacheNames.CATEGORIES,
            CacheNames.MEME_LIST, CacheNames.MEME,
            CacheNames.SEARCH
        ).forEach(name ->
            Optional.ofNullable(cacheManager.getCache(name)).ifPresent(c -> c.clear())
        );
        log.info("All caches invalidated");
        return null;
    }

    public void invalidateAll() {
        execute(null);
    }
}
