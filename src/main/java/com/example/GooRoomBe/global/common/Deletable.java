package com.example.GooRoomBe.global.common;

import lombok.Getter;

@Getter
public abstract class Deletable<T> {
    private final T entity;

    protected Deletable(T entity) {
        if (entity == null) throw new IllegalArgumentException("Target cannot be null");
        this.entity = entity;
    }
}