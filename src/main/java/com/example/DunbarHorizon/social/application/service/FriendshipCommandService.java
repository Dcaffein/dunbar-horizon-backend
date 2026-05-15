package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.application.port.in.FriendshipCommandUseCase;
import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class FriendshipCommandService implements FriendshipCommandUseCase {
    private final FriendshipRepository friendshipRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Neo4jTransactional
    public void updateFriendship(Long currentUserId, Long friendId, FriendshipUpdateCommand command) {
        Friendship friendship = findById(currentUserId, friendId);
        friendship.updateUserFields(currentUserId, command.friendAlias(), command.isMuted(), command.isRoutable());
        friendshipRepository.updateUserFields(friendship, currentUserId);
    }

    @Neo4jTransactional
    public void brokeUpWith(Long currentUserId, Long friendId) {
        Friendship friendShip = findById(currentUserId, friendId);
        List<UserReference> userIds = friendShip.getUsers().stream().toList();

        friendshipRepository.delete(friendShip.getId());

        eventPublisher.publishEvent(new FriendShipDeletedEvent(userIds.getFirst().getId(), userIds.getLast().getId()));
    }

    private Friendship findById(Long userId, Long friendId) {
        String friendshipId = Friendship.generateCompositeId(userId, friendId);
        return friendshipRepository.findById(friendshipId).orElseThrow(()-> new FriendshipNotFoundException(userId, friendId));
    }
}
