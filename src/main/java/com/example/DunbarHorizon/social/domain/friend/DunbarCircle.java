package com.example.DunbarHorizon.social.domain.friend;

public enum DunbarCircle {
    SUPPORT(5),
    SYMPATHY(15),
    KINSHIP(50),
    DUNBAR(150);

    private final int limitSize;

    DunbarCircle(int limitSize) {
        this.limitSize = limitSize;
    }

    public int getLimitSize() {
        return limitSize;
    }
}