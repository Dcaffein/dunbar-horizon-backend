package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.FriendshipDetailResult;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FriendshipQueryUseCase {
    boolean areFriends(Long userId, Long targetId);

    FriendshipDetailResult getFriend(Long userId, Long targetId);

    List<FriendshipDetailResult> getDetailedFriendships(Long userId);

    Set<Long> getFriendIdsIn(Long userId, Collection<Long> targetIds);

    Set<Long> getMutedIds(Long userId);

    Set<Long> getListenableFriendIds(Long userId);
}
