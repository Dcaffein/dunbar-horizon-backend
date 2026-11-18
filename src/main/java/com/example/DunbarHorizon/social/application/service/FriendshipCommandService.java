package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.command.FriendshipUpdateCommand;
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
public class FriendshipCommandService {

    private final FriendshipRepository friendshipRepository;
    private final LabelMembershipRepository labelMembershipRepository;

    @Transactional
    public void updateFriendship(Long currentUserId, Long friendId, FriendshipUpdateCommand command) {
        Friendship friendship = friendshipRepository.findBetween(currentUserId, friendId)
                .orElseThrow(() -> new FriendshipNotFoundException(currentUserId, friendId));
        friendship.updateSettings(currentUserId, command.friendAlias(), command.isMuted(), command.isRoutable());
        friendshipRepository.saveSettings(friendship);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void brokeUpWith(Long currentUserId, Long friendId) {
        if (!friendshipRepository.deleteBetween(currentUserId, friendId)) {
            throw new FriendshipNotFoundException(currentUserId, friendId);
        }
        labelMembershipRepository.removeMembersBetween(currentUserId, friendId);
    }
}
