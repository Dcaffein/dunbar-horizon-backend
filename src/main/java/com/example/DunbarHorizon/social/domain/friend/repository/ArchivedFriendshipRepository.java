package com.example.DunbarHorizon.social.domain.friend.repository;

import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ArchivedFriendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchiveCandidate;

import java.util.List;

public interface ArchivedFriendshipRepository {
    void saveAll(List<ArchivedFriendship> friendships);

    default void saveCandidates(List<FriendshipArchiveCandidate> candidates) {
        saveAll(candidates.stream()
                .map(candidate -> new ArchivedFriendship(candidate.id(), candidate.userAId(), candidate.userBId(),
                        candidate.friendedAt()))
                .toList());
    }
}
