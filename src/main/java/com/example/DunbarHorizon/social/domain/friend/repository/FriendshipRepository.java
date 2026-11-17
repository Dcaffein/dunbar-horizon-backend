package com.example.DunbarHorizon.social.domain.friend.repository;

import com.example.DunbarHorizon.social.domain.friend.Friendship;

import java.util.Optional;

public interface FriendshipRepository {
    void create(Friendship friendship);

    Optional<Friendship> findBetween(Long userId, Long friendId);

    void saveSettings(Friendship friendship);

    boolean existsFriendshipBetween(Long userId, Long friendId);

    boolean deleteBetween(Long userId, Long friendId);
}
