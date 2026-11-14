package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.domain.friend.Friendship;

public interface FriendshipCreationRepository {
    void create(Friendship friendship);
    boolean existsFriendshipBetween(Long userId, Long targetId);
    boolean existsFriendshipForUpdate(long userId, long targetId);
}
