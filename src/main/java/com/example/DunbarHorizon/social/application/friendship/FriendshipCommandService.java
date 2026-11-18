package com.example.DunbarHorizon.social.application.friendship;

import com.example.DunbarHorizon.social.application.friendship.dto.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.domain.friendship.Friendship;
import com.example.DunbarHorizon.social.domain.friendship.event.FriendshipEndedEvent;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friendship.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendshipCommandService {

    private final FriendshipRepository friendshipRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void updateSettings(Long currentUserId, Long friendId, FriendshipUpdateCommand command) {
        Friendship friendship = friendshipRepository.findBetween(currentUserId, friendId)
                .orElseThrow(() -> new FriendshipNotFoundException(currentUserId, friendId));
        friendship.updateSettings(currentUserId, command.friendAlias(), command.isMuted(), command.isRoutable());
        friendshipRepository.saveSettings(friendship);
    }

    @Transactional
    public void endFriendship(Long currentUserId, Long friendId) {
        if (!friendshipRepository.deleteBetween(currentUserId, friendId)) {
            throw new FriendshipNotFoundException(currentUserId, friendId);
        }
        applicationEventPublisher.publishEvent(new FriendshipEndedEvent(currentUserId, friendId));
    }
}
