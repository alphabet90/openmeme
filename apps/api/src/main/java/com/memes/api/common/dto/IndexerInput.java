package com.memes.api.common.dto;

import com.memes.api.generated.model.MemeIndexRequest;
import lombok.Value;
import org.springframework.lang.Nullable;

@Value
public class IndexerInput {
    @Nullable MemeIndexRequest singleMemeRequest;
    boolean indexMemes;
    boolean indexCategories;
}
