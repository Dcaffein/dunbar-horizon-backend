package com.example.DunbarHorizon.social.application.port.out;

import java.util.Collection;
import java.util.Set;

public interface FriendshipLockRepository {
    Set<Long> findFriendIdsForShare(Long userId, Collection<Long> friendIds);
}
