package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.FriendshipDetailResult;
import com.example.DunbarHorizon.social.application.port.in.FriendshipQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Neo4jTransactional(readOnly = true)
public class FriendshipQueryService implements FriendshipQueryUseCase {

    private final FriendshipRepository friendshipRepository;
    private final ImageUrlResolverPort imageUrlResolverPort;

    @Override
    public List<FriendshipDetailResult> getDetailedFriendships(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByUserId(userId);

        return friendships.stream()
                .map(friendship -> FriendshipDetailResult.from(friendship, userId, imageUrlResolverPort))
                .toList();
    }

    @Override
    public boolean areFriends(Long userId, Long targetId) {
        return friendshipRepository.existsFriendshipBetween(userId, targetId);
    }

    @Override
    public FriendshipDetailResult getFriend(Long userId, Long targetId) {
        String friendshipId = Friendship.generateCompositeId(userId, targetId);
        Friendship friendship = friendshipRepository.findById(friendshipId).orElseThrow(() -> new FriendshipNotFoundException(userId, targetId));
        return FriendshipDetailResult.from(friendship, userId, imageUrlResolverPort);
    }

    @Override
    public Set<Long> getFriendIdsIn(Long userId, Collection<Long> targetIds) {
        return friendshipRepository.filterFriendIdsAmong(userId, targetIds);
    }

    @Override
    public Set<Long> getMutedIds(Long userId) {
        return friendshipRepository.findFriendIdsByMuteStatus(userId, true);
    }

    @Override
    public Set<Long> getListenableFriendIds(Long userId) {
        return friendshipRepository.findFriendIdsByMuteStatus(userId, false);
    }
}
