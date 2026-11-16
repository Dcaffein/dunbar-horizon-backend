package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.port.in.FriendshipCommandUseCase;
import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.application.port.out.LabelMembershipRepository;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendshipCommandService implements FriendshipCommandUseCase {

    private final FriendshipRepository friendships;
    private final LabelMembershipRepository labels;

    @Override
    @Transactional(transactionManager = "transactionManager", isolation = Isolation.READ_COMMITTED)
    public void updateFriendship(Long currentUserId, Long friendId, FriendshipUpdateCommand command) {
        Friendship friendship = friendships.findBetween(currentUserId, friendId)
                .orElseThrow(() -> new FriendshipNotFoundException(currentUserId, friendId));
        friendship.updateUserFields(currentUserId, command.friendAlias(), command.isMuted(), command.isRoutable());
        friendships.updateUserFields(friendship, currentUserId);
    }

    @Override
    @Transactional(transactionManager = "transactionManager", isolation = Isolation.READ_COMMITTED)
    public void brokeUpWith(Long currentUserId, Long friendId) {
        Friendship locked = friendships.findBetweenForUpdate(currentUserId, friendId)
                .orElseThrow(() -> new FriendshipNotFoundException(currentUserId, friendId));
        removeFriendships(List.of(locked));
    }

    private void removeFriendships(List<Friendship> locked) {
        labels.removeMembersForEndedFriendships(locked);
        friendships.deleteAll(locked);
    }
}
