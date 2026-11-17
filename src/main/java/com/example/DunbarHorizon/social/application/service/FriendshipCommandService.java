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

@Service
@RequiredArgsConstructor
public class FriendshipCommandService implements FriendshipCommandUseCase {

    private final FriendshipRepository friendships;
    private final LabelMembershipRepository labels;

    @Override
    @Transactional(transactionManager = "transactionManager")
    public void updateFriendship(Long currentUserId, Long friendId, FriendshipUpdateCommand command) {
        Friendship friendship = friendships.findBetween(currentUserId, friendId)
                .orElseThrow(() -> new FriendshipNotFoundException(currentUserId, friendId));
        friendship.updateSettings(currentUserId, command.friendAlias(), command.isMuted(), command.isRoutable());
        friendships.saveSettings(friendship);
    }

    @Override
    @Transactional(transactionManager = "transactionManager", isolation = Isolation.READ_COMMITTED)
    public void brokeUpWith(Long currentUserId, Long friendId) {
        if (!friendships.deleteBetween(currentUserId, friendId)) {
            throw new FriendshipNotFoundException(currentUserId, friendId);
        }
        labels.removeMembersBetween(currentUserId, friendId);
    }
}
