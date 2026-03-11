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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Service
public class FriendshipCommandService implements FriendshipCommandUseCase {
    private final FriendshipRepository friendshipRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void updateFriendship(Long currentUserId, Long friendId, FriendshipUpdateCommand command) {
        Friendship friendship = findById(currentUserId,friendId);

        applyIfPresent(command.friendAlias(), alias -> friendship.updateFriendAlias(currentUserId, alias));
        applyIfPresent(command.isMuted(), alias -> friendship.updateMuteStatus(currentUserId, alias));
        applyIfPresent(command.isRoutable(), alias -> friendship.updateRoutableStatus(currentUserId, alias));

        friendshipRepository.save(friendship);
    }

    @Transactional
    public void brokeUpWith(Long currentUserId, Long friendId) {
        Friendship friendShip = findById(currentUserId,friendId);

        friendShip.checkDeletable(currentUserId);
        List<UserReference> userIds = friendShip.getUsers().stream().toList();
        friendshipRepository.delete(friendShip);

        eventPublisher.publishEvent(new FriendShipDeletedEvent(userIds.getFirst().getId(), userIds.getLast().getId()));
    }

    private <T> void applyIfPresent(T value, Consumer<T> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    private Friendship findById(Long userId, Long friendId) {
        String friendshipId = Friendship.generateCompositeId(userId, friendId);
        return friendshipRepository.findById(friendshipId).orElseThrow(()-> new FriendshipNotFoundException(userId, friendId));
    }
}
