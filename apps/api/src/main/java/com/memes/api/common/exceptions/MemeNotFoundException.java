package com.memes.api.common.exceptions;

public class MemeNotFoundException extends RuntimeException {
    public MemeNotFoundException(String category, String slug) {
        super("Meme not found: " + category + "/" + slug);
    }
}
