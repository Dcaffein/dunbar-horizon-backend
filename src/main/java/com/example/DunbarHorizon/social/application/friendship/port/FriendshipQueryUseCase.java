package com.example.DunbarHorizon.social.application.friendship.port;

import java.util.Collection;
import java.util.Set;

public interface FriendshipQueryUseCase {
    boolean areFriends(Long userId, Long targetId);

    Set<Long> getFriendIdsIn(Long userId, Collection<Long> targetIds);

    Set<Long> getMutedIds(Long userId);

    Set<Long> getListenableFriendIds(Long userId);
}
