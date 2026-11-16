package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.FriendshipDetailResult;
import com.example.DunbarHorizon.social.application.port.out.FriendshipIdRepository;
import com.example.DunbarHorizon.social.application.port.out.SocialUserProfileReader;
import com.example.DunbarHorizon.social.application.port.in.FriendshipQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipQueryService implements FriendshipQueryUseCase {

    private final FriendshipRepository friendshipRepository;
    private final FriendshipIdRepository friendshipIdRepository;
    private final ImageUrlResolverPort imageUrlResolverPort;
    private final SocialUserProfileReader profiles;

    @Override
    public List<FriendshipDetailResult> getDetailedFriendships(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByUserId(userId);
        var friends = profiles.findActiveProfiles(friendships.stream().map(friendship -> friendship.getFriendId(userId)).toList());

        return friendships.stream()
                .map(friendship -> FriendshipDetailResult.from(friendship, userId, friends.get(friendship.getFriendId(userId)), imageUrlResolverPort))
                .toList();
    }

    @Override
    public boolean areFriends(Long userId, Long targetId) {
        return friendshipRepository.existsFriendshipBetween(userId, targetId);
    }

    @Override
    public FriendshipDetailResult getFriend(Long userId, Long targetId) {
        Friendship friendship = friendshipRepository.findBetween(userId, targetId).orElseThrow(() -> new FriendshipNotFoundException(userId, targetId));
        var friend = profiles.findActiveProfiles(List.of(targetId)).get(targetId);
        return FriendshipDetailResult.from(friendship, userId, friend, imageUrlResolverPort);
    }

    @Override
    public Set<Long> getFriendIdsIn(Long userId, Collection<Long> targetIds) {
        return friendshipIdRepository.filterFriendIdsAmong(userId, targetIds);
    }

    @Override
    public Set<Long> getMutedIds(Long userId) {
        return friendshipIdRepository.findFriendIdsByMuteStatus(userId, true);
    }

    @Override
    public Set<Long> getListenableFriendIds(Long userId) {
        return friendshipIdRepository.findFriendIdsByMuteStatus(userId, false);
    }
}
