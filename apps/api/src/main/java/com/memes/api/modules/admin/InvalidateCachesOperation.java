package com.memes.api.modules.admin;

import com.memes.api.config.RedisConfig;
import com.memes.api.common.operation.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InvalidateCachesOperation implements Operation<Void, Void> {

    private final CacheManager cacheManager;

    @Override
    public Void execute(Void input) {
        List.of(
            RedisConfig.CACHE_STATS, RedisConfig.CACHE_CATEGORIES,
            RedisConfig.CACHE_MEME_LIST, RedisConfig.CACHE_MEME,
            RedisConfig.CACHE_SEARCH
        ).forEach(name ->
            Optional.ofNullable(cacheManager.getCache(name)).ifPresent(org.springframework.cache.Cache::clear)
        );
        return null;
    }
}
