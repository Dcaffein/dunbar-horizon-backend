package com.example.GooRoomBe.social.application.service;

import com.example.GooRoomBe.social.application.port.in.FriendshipQueryUseCase;
import com.example.GooRoomBe.social.application.port.in.dto.FriendProfile;
import com.example.GooRoomBe.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipQueryService implements FriendshipQueryUseCase {

    private final FriendshipRepository friendshipRepository;

    @Override
    public boolean areFriends(Long userId, Long targetId) {
        return friendshipRepository.existsFriendshipBetween(userId, targetId);
    }

    @Override
    public Set<Long> getAllFriendIds(Long userId) {
        return friendshipRepository.findFriendIds(userId);
    }

    @Override
    public Set<FriendProfile> getAllFriends(Long userId) {
        return friendshipRepository.findFriends(userId).stream()
                .map(FriendProfile::from)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getFriendIdsIn(Long userId, Collection<Long> targetIds) {
        return friendshipRepository.findFriendIdsIn(userId, targetIds);
    }

    @Override
    public Set<FriendProfile> getFriendProfilesIn(Long userId, Collection<Long> targetIds) {
        return friendshipRepository.findFriendsIn(userId, targetIds).stream()
                .map(FriendProfile::from)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getMutedIds(Long userId) {
        return friendshipRepository.findFriendIdsByMuteStatus(userId, true);
    }

    @Override
    public Set<FriendProfile> getListenableFriends(Long userId) {
        return friendshipRepository.findFriendsByMuteStatus(userId, false).stream()
                .map(FriendProfile::from)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getListenableFriendIds(Long userId) {
        return friendshipRepository.findFriendIdsByMuteStatus(userId, false);
    }
}
