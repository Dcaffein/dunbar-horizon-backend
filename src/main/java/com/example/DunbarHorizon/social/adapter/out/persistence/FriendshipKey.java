package com.example.DunbarHorizon.social.adapter.out.persistence;

public record FriendshipKey(long low, long high) {

    public static FriendshipKey of(long first, long second) {
        return new FriendshipKey(Math.min(first, second), Math.max(first, second));
    }

    boolean isPair() {
        return low < high;
    }
}
