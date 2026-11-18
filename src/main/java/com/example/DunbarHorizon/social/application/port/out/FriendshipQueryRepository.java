package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.FriendView;
import com.example.DunbarHorizon.social.application.dto.UserProfile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FriendshipQueryRepository {
    List<FriendView> findFriends(Long userId);

    Optional<FriendView> findFriend(Long userId, Long friendId);

    Set<Long> findFriendIdsIn(Long userId, Collection<Long> candidateIds);

    Set<Long> findFriendIdsByMuteStatus(Long userId, boolean muted);

    Optional<UserProfile> findActiveProfile(Long userId);
}
