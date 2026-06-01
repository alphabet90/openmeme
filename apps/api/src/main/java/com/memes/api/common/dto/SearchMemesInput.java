package com.memes.api.common.dto;

public record SearchMemesInput(String query, int page, int limit, String locale) {}
