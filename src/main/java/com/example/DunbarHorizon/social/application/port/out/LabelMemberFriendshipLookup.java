package com.example.DunbarHorizon.social.application.port.out;

import java.util.Collection;
import java.util.Set;

public interface LabelMemberFriendshipLookup {
    /**
     * Returns active friends among the candidates and holds shared locks on their relationships
     * until the caller's transaction ends.
     */
    Set<Long> findFriendIdsWithSharedLock(Long ownerId, Collection<Long> candidateIds);
}
