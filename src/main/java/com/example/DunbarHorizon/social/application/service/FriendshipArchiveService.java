package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ArchivedFriendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchiveCandidate;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchivePolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.ArchivedFriendshipRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendshipArchiveService {

    private final FriendshipRepository friendshipRepository;
    private final ArchivedFriendshipRepository archivedFriendshipRepository;
    private final FriendshipArchivePolicy archivePolicy;

    @Transactional
    public List<String> archiveFriendships() {
        double threshold = archivePolicy.archiveThreshold();
        List<FriendshipArchiveCandidate> candidates = friendshipRepository.findArchiveCandidates(threshold);

        if (candidates.isEmpty()) {
            return List.of();
        }

        List<ArchivedFriendship> archived = candidates.stream()
                .map(c -> new ArchivedFriendship(c.id(), c.userAId(), c.userBId(), c.friendedAt()))
                .toList();

        archivedFriendshipRepository.saveAll(archived);

        return candidates.stream().map(FriendshipArchiveCandidate::id).toList();
    }

    @Neo4jTransactional
    public void deleteArchivedFromNeo4j(Collection<String> ids) {
        if (ids.isEmpty()) return;
        friendshipRepository.deleteAllByIds(ids);
    }
}
