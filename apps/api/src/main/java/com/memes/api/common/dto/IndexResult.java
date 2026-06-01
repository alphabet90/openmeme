package com.memes.api.common.dto;

import java.util.List;

public record IndexResult(int indexed, long durationMs, List<String> errors) {}
