package com.example.DunbarHorizon.social.application.port.out;

import java.util.Collection;
import java.util.Set;

public interface LabelMemberFriendshipLookup {
    /**
     * Returns active candidates and the subset whose friendships with the owner are locked
     * in shared mode until the caller's transaction ends.
     */
    Candidates findCandidatesWithSharedLock(Long ownerId, Collection<Long> candidateIds);

    record Candidates(Set<Long> activeCandidateIds, Set<Long> lockedFriendIds) {}
}
