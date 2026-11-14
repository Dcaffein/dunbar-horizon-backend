package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.social.domain.friend.repository.ArchivedFriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ArchivedFriendshipRepositoryAdapter implements ArchivedFriendshipRepository {

    private final ArchivedFriendshipJpaRepository archivedFriendshipJpaRepository;

    @Override
    public void saveAll(List<ArchivedFriendship> friendships) {
        archivedFriendshipJpaRepository.saveAll(friendships);
    }
}
