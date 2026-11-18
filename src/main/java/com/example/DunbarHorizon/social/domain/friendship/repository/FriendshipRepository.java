package com.example.DunbarHorizon.social.domain.friendship.repository;

import com.example.DunbarHorizon.social.domain.friendship.Friendship;

import java.util.Optional;

public interface FriendshipRepository {
    void create(Friendship friendship);

    Optional<Friendship> findBetween(Long userId, Long friendId);

    void saveSettings(Friendship friendship);

    boolean existsBetween(Long userId, Long friendId);

    boolean deleteBetween(Long userId, Long friendId);
}
