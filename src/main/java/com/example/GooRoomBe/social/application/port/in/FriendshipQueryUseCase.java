package com.example.GooRoomBe.social.application.port.in;

import com.example.GooRoomBe.social.application.port.in.dto.FriendProfile;

import java.util.Collection;
import java.util.Set;

public interface FriendshipQueryUseCase {
    boolean areFriends(Long userId, Long flagId);

    Set<Long> getAllFriendIds(Long userId);
    Set<FriendProfile> getAllFriends(Long userId);

    Set<Long> getFriendIdsIn(Long userId, Collection<Long> targetIds);
    Set<FriendProfile> getFriendProfilesIn(Long userId, Collection<Long> targetIds);

    Set<Long> getMutedIds(Long userId);

    Set<FriendProfile> getListenableFriends(Long userId);
    Set<Long> getListenableFriendIds(Long userId);
}
