package com.example.DunbarHorizon.social.adapter.out.persistence.mysql;

import com.example.DunbarHorizon.social.domain.friend.Friendship;

import java.util.Map;
import java.util.Optional;

record UserPairKey(long low, long high) {

    static UserPairKey of(long first, long second) {
        return new UserPairKey(Math.min(first, second), Math.max(first, second));
    }

    static Optional<UserPairKey> parse(String id) {
        String[] ids = id.split("_", -1);
        if (ids.length != 2) {
            return Optional.empty();
        }
        try {
            var pair = of(Long.parseLong(ids[0]), Long.parseLong(ids[1]));
            return pair.low < pair.high && pair.id().equals(id) ? Optional.of(pair) : Optional.empty();
        } catch (NumberFormatException invalidId) {
            return Optional.empty();
        }
    }

    String id() {
        return Friendship.generateCompositeId(low, high);
    }

    Map<String, Long> parameters() {
        return Map.of("low", low, "high", high);
    }
}
