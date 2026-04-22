package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.FriendshipDetailResult;
import com.example.DunbarHorizon.social.application.port.in.FriendshipQueryUseCase;
import com.example.DunbarHorizon.social.application.dto.info.FriendProfileInfo;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipQueryService implements FriendshipQueryUseCase {

    private final FriendshipRepository friendshipRepository;

    @Override
    public List<FriendshipDetailResult> getDetailedFriendships(Long userId) {
        List<Friendship> friendships = friendshipRepository.findFriendships(userId);

        return friendships.stream()
                .map(friendship -> FriendshipDetailResult.from(friendship, userId))
                .toList();
    }

    @Override
    public boolean areFriends(Long userId, Long targetId) {
        return friendshipRepository.existsFriendshipBetween(userId, targetId);
    }

    @Override
    public Friendship getFriend(Long userId, Long targetId) {
        String friendshipId = Friendship.generateCompositeId(userId, targetId);
        return friendshipRepository.findById(friendshipId).orElseThrow(()-> new FriendshipNotFoundException(userId, targetId));
    }

    @Override
    public Set<Long> getAllFriendIds(Long userId) {
        return friendshipRepository.findFriendIds(userId);
    }

    @Override
    public Set<FriendProfileInfo> getAllFriends(Long userId) {
        return friendshipRepository.findFriends(userId).stream()
                .map(FriendProfileInfo::from)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getFriendIdsIn(Long userId, Collection<Long> targetIds) {
        return friendshipRepository.findFriendIdsIn(userId, targetIds);
    }

    @Override
    public Set<FriendProfileInfo> getFriendProfilesIn(Long userId, Collection<Long> targetIds) {
        return friendshipRepository.findFriendsIn(userId, targetIds).stream()
                .map(FriendProfileInfo::from)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getMutedIds(Long userId) {
        return friendshipRepository.findFriendIdsByMuteStatus(userId, true);
    }

    @Override
    public Set<FriendProfileInfo> getListenableFriends(Long userId) {
        return friendshipRepository.findFriendsByMuteStatus(userId, false).stream()
                .map(FriendProfileInfo::from)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getListenableFriendIds(Long userId) {
        return friendshipRepository.findFriendIdsByMuteStatus(userId, false);
    }
}
