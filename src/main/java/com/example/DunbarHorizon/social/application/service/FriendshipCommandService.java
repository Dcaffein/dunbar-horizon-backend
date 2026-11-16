package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.port.in.FriendshipCommandUseCase;
import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.application.port.out.FriendshipLifecycleRepository;
import com.example.DunbarHorizon.social.application.port.out.LabelMembershipRepository;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchiveCandidate;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchivePolicy;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.ArchivedFriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendshipCommandService implements FriendshipCommandUseCase {

    private final FriendshipLifecycleRepository friendships;
    private final LabelMembershipRepository labels;
    private final ArchivedFriendshipRepository archives;
    private final FriendshipArchivePolicy archivePolicy;

    @Override
    public void updateFriendship(Long currentUserId, Long friendId, FriendshipUpdateCommand command) {
        friendships.updateUserFields(currentUserId, friendId, command.friendAlias(), command.isMuted(),
                command.isRoutable());
    }

    @Override
    @Transactional(transactionManager = "transactionManager", isolation = Isolation.READ_COMMITTED)
    public void brokeUpWith(Long currentUserId, Long friendId) {
        String id = Friendship.generateCompositeId(currentUserId, friendId);
        List<Friendship> locked = friendships.findAllByIdsForUpdate(List.of(id));
        if (locked.isEmpty()) {
            throw new FriendshipNotFoundException(currentUserId, friendId);
        }
        removeFriendships(locked);
    }

    @Transactional(transactionManager = "transactionManager", isolation = Isolation.READ_COMMITTED)
    public void archiveFriendships() {
        double threshold = archivePolicy.archiveThreshold();
        List<FriendshipArchiveCandidate> candidates = friendships.findArchiveCandidates(threshold);
        List<String> ids = candidates.stream().map(FriendshipArchiveCandidate::id).toList();
        List<Friendship> eligible = friendships.findAllByIdsForUpdate(ids).stream()
                .filter(friendship -> friendship.getIntimacy() <= threshold)
                .toList();
        if (eligible.isEmpty()) {
            return;
        }

        archives.archive(eligible);
        removeFriendships(eligible);
    }

    private void removeFriendships(List<Friendship> locked) {
        labels.removeMembersForEndedFriendships(locked);
        friendships.deleteAllByIds(locked.stream().map(Friendship::getId).toList());
    }
}
