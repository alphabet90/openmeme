package com.memes.api.common.dto;

import java.util.List;
import lombok.Data;

@Data
public class IndexResult {
    private final int indexed;
    private final long durationMs;
    private final List<String> errors;
}
