package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.application.port.out.LabelMemberFriendshipLookup;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LabelMemberFriendshipLookupJpaAdapter implements LabelMemberFriendshipLookup {

    private final FriendshipJpaRepository friendshipJpaRepository;

    @Override
    public Set<Long> findForMemberAdditionWithSharedLock(Long ownerId, Long memberId) {
        Set<Long> lockedFriendIds = findFriendIdsWithSharedLock(ownerId, List.of(memberId));
        if (lockedFriendIds.isEmpty()) {
            friendshipJpaRepository.findAvailableMemberId(memberId)
                    .orElseThrow(() -> new UserReferenceNotFoundException(memberId));
        }
        return lockedFriendIds;
    }

    @Override
    public Set<Long> findFriendIdsWithSharedLock(Long ownerId, Collection<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return Set.of();
        }
        return friendshipJpaRepository.findAllBetweenForShare(ownerId, candidateIds).stream()
                .map(friendship -> friendship.getFriendId(ownerId))
                .collect(Collectors.toUnmodifiableSet());
    }
}
