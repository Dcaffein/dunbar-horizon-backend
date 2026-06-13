package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.port.in.command.FriendshipUpdateCommand;
import com.example.DunbarHorizon.social.application.service.FriendshipCommandService;
import com.example.DunbarHorizon.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipNotFoundException;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @InjectMocks
    private FriendshipCommandService friendshipService;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("별명만 수정을 요청하면 Mute와 Routable 수정 메서드는 호출되지 않아야 한다")
    void updateFriendship_OnlyAlias() {
        // given
        Long myId = 1L;
        Long friendId = 2L;
        String compositeId = Friendship.generateCompositeId(myId, friendId);

        Friendship mockFriendship = mock(Friendship.class);

        // 별명만 있고 나머지는 null인 command
        FriendshipUpdateCommand command = FriendshipUpdateCommand.builder()
                .currentUserId(myId)
                .friendId(friendId)
                .friendAlias("새별명")
                .isMuted(null)    // 명시적 null
                .isRoutable(null) // 명시적 null
                .build();

        given(friendshipRepository.findById(compositeId)).willReturn(Optional.of(mockFriendship));

        // when
        friendshipService.updateFriendship(myId, friendId, command);

        // then
        verify(mockFriendship).updateUserFields(myId, "새별명", null, null);
        verify(friendshipRepository).updateUserFields(mockFriendship, myId);
    }

    @Test
    @DisplayName("Mute와 Routable 상태만 변경하면 별명 수정 메서드는 호출되지 않아야 한다")
    void updateFriendship_StatusOnly() {
        // given
        Long myId = 1L;
        Long friendId = 2L;
        String compositeId = Friendship.generateCompositeId(myId, friendId);

        Friendship mockFriendship = mock(Friendship.class);

        // 상태값만 있고 별명은 null인 command
        FriendshipUpdateCommand command = FriendshipUpdateCommand.builder()
                .currentUserId(myId)
                .friendId(friendId)
                .friendAlias(null)
                .isMuted(true)
                .isRoutable(false)
                .build();

        given(friendshipRepository.findById(compositeId)).willReturn(Optional.of(mockFriendship));

        // when
        friendshipService.updateFriendship(myId, friendId, command);

        // then
        verify(mockFriendship).updateUserFields(myId, null, true, false);
        verify(friendshipRepository).updateUserFields(mockFriendship, myId);
    }
    @Test
    @DisplayName("별명으로 빈 문자열을 전달하면 updateUserFields에 빈 문자열이 그대로 전달된다")
    void updateFriendship_withEmptyAlias_passesEmptyStringToUpdateUserFields() {
        // given
        Long myId = 1L;
        Long friendId = 2L;
        String compositeId = Friendship.generateCompositeId(myId, friendId);

        Friendship mockFriendship = mock(Friendship.class);

        FriendshipUpdateCommand command = FriendshipUpdateCommand.builder()
                .currentUserId(myId)
                .friendId(friendId)
                .friendAlias("")
                .isMuted(null)
                .isRoutable(null)
                .build();

        given(friendshipRepository.findById(compositeId)).willReturn(Optional.of(mockFriendship));

        // when
        friendshipService.updateFriendship(myId, friendId, command);

        // then — 도메인 내부에서 "" → null 변환; updateUserFields는 "" 그대로 수신
        verify(mockFriendship).updateUserFields(myId, "", null, null);
        verify(friendshipRepository).updateUserFields(mockFriendship, myId);
    }

    @Test
    @DisplayName("존재하지 않는 친구 관계를 해제하려 하면 FriendshipNotFoundException이 발생한다")
    void brokeUpWith_FriendshipNotFound_ThrowsException() {
        // given
        Long myId = 1L;
        Long friendId = 99L;
        String compositeId = Friendship.generateCompositeId(myId, friendId);

        given(friendshipRepository.findById(compositeId)).willReturn(Optional.empty());

        // when & then
        Assertions.assertThrows(
                FriendshipNotFoundException.class,
                () -> friendshipService.brokeUpWith(myId, friendId)
        );
        verify(friendshipRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("친구 관계 해제 시 복합 ID로 조회 후 물리 삭제하며 이벤트를 발행한다")
    void brokeUpWith_Success() {
        // given
        Long myId = 1L;
        Long friendId = 2L;
        String compositeId = Friendship.generateCompositeId(myId, friendId);

        Friendship mockFriendship = mock(Friendship.class);
        SocialUser u1 = new SocialUser(1L, "u1", "");
        SocialUser u2 = new SocialUser(2L, "u2", "");

        given(friendshipRepository.findById(compositeId)).willReturn(Optional.of(mockFriendship));
        given(mockFriendship.getId()).willReturn(compositeId);
        given(mockFriendship.getUsers()).willReturn(Set.of(u1, u2));

        // when
        friendshipService.brokeUpWith(myId, friendId);

        // then
        verify(friendshipRepository).delete(compositeId);
        verify(eventPublisher).publishEvent(any(FriendShipDeletedEvent.class));
    }
}