package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.FriendshipDetailResult;
import com.example.DunbarHorizon.social.application.port.out.FriendViewRepository;
import com.example.DunbarHorizon.social.application.port.out.FriendshipIdRepository;
import com.example.DunbarHorizon.social.application.port.in.FriendshipQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.ImageUrlResolverPort;
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
    private final FriendViewRepository friendViewRepository;
    private final ImageUrlResolverPort imageUrlResolverPort;

    public List<FriendshipDetailResult> getDetailedFriendships(Long userId) {
        return friendViewRepository.findFriends(userId).stream()
                .map(friend -> FriendshipDetailResult.from(friend, imageUrlResolverPort))
                .toList();
    }

    @Override
    public boolean areFriends(Long userId, Long targetId) {
        return friendshipRepository.existsFriendshipBetween(userId, targetId);
    }

    public FriendshipDetailResult getFriend(Long userId, Long targetId) {
        return friendViewRepository.findFriend(userId, targetId)
                .map(friend -> FriendshipDetailResult.from(friend, imageUrlResolverPort))
                .orElseThrow(() -> new FriendshipNotFoundException(userId, targetId));
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
