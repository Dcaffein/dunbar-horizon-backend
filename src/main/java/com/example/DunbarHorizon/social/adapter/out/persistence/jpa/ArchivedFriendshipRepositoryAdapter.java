package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.ArchivedFriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
@RequiredArgsConstructor
public class ArchivedFriendshipRepositoryAdapter implements ArchivedFriendshipRepository {

    private final ArchivedFriendshipJpaRepository archivedFriendshipJpaRepository;

    @Override
    public void archive(Collection<Friendship> friendships) {
        archivedFriendshipJpaRepository.saveAll(friendships.stream().map(ArchivedFriendshipRepositoryAdapter::toRecord).toList());
    }

    private static ArchivedFriendship toRecord(Friendship friendship) {
        Long low = friendship.getLower().getUserId();
        Long high = friendship.getHigher().getUserId();
        return new ArchivedFriendship(low + "_" + high, low, high, friendship.getCreatedAt());
    }
}
