package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import com.example.DunbarHorizon.social.application.eventHandler.FriendInteractionEventListener;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendInteractionEventListenerTest {

    @InjectMocks
    private FriendInteractionEventListener interactionListener;
    @Mock
    private FriendshipRepository friendshipRepository;

    Long requesterId = 1L;
    Long receiverId = 2L;
    double score = 1.0;
    String compositeId = Friendship.generateCompositeId(requesterId, receiverId);

    @Test
    @DisplayName("상호작용 이벤트 수신 시 점수를 조절하고 저장한다")
    void handleUserInteraction_Success() {
        // given

        UserInteractionEvent event = new UserInteractionEvent(requesterId, receiverId, InteractionType.VISIT);
        Friendship mockFriendship = mock(Friendship.class);

        given(friendshipRepository.findById(compositeId))
                .willReturn(Optional.of(mockFriendship));

        // when
        interactionListener.handleUserInteraction(event);

        // then
        verify(mockFriendship).adjustInterestScore(requesterId, score);
        verify(friendshipRepository).save(mockFriendship);
    }

    @Test
    @DisplayName("친구 관계가 존재하지 않아도 예외를 던지지 않고 종료한다")
    void handleUserInteraction_NotFound_SwallowsException() {
        // given
        UserInteractionEvent event = new UserInteractionEvent(requesterId, receiverId, InteractionType.VISIT);
        given(friendshipRepository.findById(compositeId)).willReturn(Optional.empty());

        // when & then
        // catch 블록이 잘 작동하여 외부로 예외가 나가지 않는지 검증
        assertDoesNotThrow(() -> interactionListener.handleUserInteraction(event));
        verify(friendshipRepository, never()).save(any());
    }
}