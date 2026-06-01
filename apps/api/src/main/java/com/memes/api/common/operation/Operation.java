package com.memes.api.common.operation;

public interface Operation<I, O> {
    O execute(I input);
}
