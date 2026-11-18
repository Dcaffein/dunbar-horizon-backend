package com.example.DunbarHorizon.social.application.label.port;

import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;

import java.util.Collection;
import java.util.Set;

public interface LabelMemberFriendshipLookup {
    /**
     * Returns the locked friend ID, or an empty set if the available member is not a friend.
     * @throws UserReferenceNotFoundException if the member is unavailable
     */
    Set<Long> findForMemberAdditionWithSharedLock(Long ownerId, Long memberId);

    /** Returns IDs whose friendships with the owner are locked until the caller's transaction ends. */
    Set<Long> findFriendIdsWithSharedLock(Long ownerId, Collection<Long> candidateIds);
}
