package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.info.FriendProfileInfo;
import com.example.DunbarHorizon.social.application.dto.result.FriendshipDetailResult;
import com.example.DunbarHorizon.social.domain.friend.Friendship;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface FriendshipQueryUseCase {
    boolean areFriends(Long userId, Long targetId);

    Friendship getFriend(Long userId, Long targetId);

    List<FriendshipDetailResult> getDetailedFriendships(Long userId);

    Set<Long> getAllFriendIds(Long userId);
    Set<FriendProfileInfo> getAllFriends(Long userId);

    Set<Long> getFriendIdsIn(Long userId, Collection<Long> targetIds);
    Set<FriendProfileInfo> getFriendProfilesIn(Long userId, Collection<Long> targetIds);

    Set<Long> getMutedIds(Long userId);

    Set<FriendProfileInfo> getListenableFriends(Long userId);
    Set<Long> getListenableFriendIds(Long userId);
}
