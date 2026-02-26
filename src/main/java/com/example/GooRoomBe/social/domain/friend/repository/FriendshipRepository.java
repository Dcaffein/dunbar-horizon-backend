package com.example.GooRoomBe.social.domain.friend.repository;

import com.example.GooRoomBe.social.domain.friend.Friendship;
import com.example.GooRoomBe.social.domain.socialUser.UserReference;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface FriendshipRepository {
    Friendship save(Friendship friendship);
    Optional<Friendship> findById(String id);
    void delete(Friendship friendship);

    boolean existsFriendshipBetween(Long userId, Long targetId);

    Set<Long> findFriendIds(Long userId);
    Set<UserReference> findFriends(Long userId);

    Set<Long> findFriendIdsIn(Long userId, Collection<Long> candidateIds);
    Set<UserReference> findFriendsIn(Long userId, Collection<Long> targetIds);

    Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted);
    Set<UserReference> findFriendsByMuteStatus(Long userId, boolean isMuted);

    void applyDecay(double rate, double threshold, LocalDateTime decayTime);
}