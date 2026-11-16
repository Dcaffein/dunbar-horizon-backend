package com.example.DunbarHorizon.social.application.port.out;

import java.util.Collection;
import java.util.Set;

public interface FriendshipIdRepository {
    Set<Long> findFriendIdsByMuteStatus(Long userId, boolean isMuted);

    Set<Long> filterFriendIdsAmong(Long userId, Collection<Long> candidateIds);
}
