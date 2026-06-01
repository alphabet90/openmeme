package com.memes.api.common.dto;

import com.memes.api.generated.model.MemeIndexRequest;

public record IndexMemeInput(
    MemeIndexRequest request,
    boolean indexMemes,
    boolean indexCategories
) {

    public IndexMemeInput(MemeIndexRequest request) {
        this(request, true, true);
    }
}
