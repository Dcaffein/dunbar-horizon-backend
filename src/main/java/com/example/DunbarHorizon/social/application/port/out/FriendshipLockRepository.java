package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.domain.friend.Friendship;

import java.util.Collection;
import java.util.List;

public interface FriendshipLockRepository {
    boolean existsFriendshipForUpdate(Long userId, Long friendId);

    List<Friendship> findAllBetweenForUpdate(Long userId, Collection<Long> friendIds);
}
