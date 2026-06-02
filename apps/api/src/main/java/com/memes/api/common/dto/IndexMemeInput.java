package com.memes.api.common.dto;

import com.memes.api.generated.model.MemeIndexRequest;
import lombok.Data;

@Data
public class IndexMemeInput {
    private final MemeIndexRequest request;
    private final boolean indexMemes;
    private final boolean indexCategories;

    public IndexMemeInput(MemeIndexRequest request) {
        this(request, true, true);
    }

    public IndexMemeInput(MemeIndexRequest request, boolean indexMemes, boolean indexCategories) {
        this.request = request;
        this.indexMemes = indexMemes;
        this.indexCategories = indexCategories;
    }
}
