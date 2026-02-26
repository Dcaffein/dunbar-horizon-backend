package com.example.GooRoomBe.social.application;

import com.example.GooRoomBe.social.application.port.in.dto.FriendshipUpdateCommand;
import com.example.GooRoomBe.social.application.service.FriendshipCommandService;
import com.example.GooRoomBe.social.domain.friend.event.FriendShipDeletedEvent;
import com.example.GooRoomBe.social.domain.friend.Friendship;
import com.example.GooRoomBe.social.domain.friend.repository.FriendshipRepository;
import com.example.GooRoomBe.social.domain.socialUser.SocialUser;
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
        // 1. 별명 수정은 호출되어야 함
        verify(mockFriendship).updateFriendAlias(myId, "새별명");

        // 2. null인 필드들은 절대 호출되면 안 됨 (핵심 검증)
        verify(mockFriendship, never()).updateMuteStatus(anyLong(), anyBoolean());
        verify(mockFriendship, never()).updateRoutableStatus(anyLong(), anyBoolean());

        verify(friendshipRepository).save(mockFriendship);
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
        // 1. 별명 수정은 호출되지 않아야 함
        verify(mockFriendship, never()).updateFriendAlias(anyLong(), anyString());

        // 2. 상태 수정 메서드들은 호출되어야 함
        verify(mockFriendship).updateMuteStatus(myId, true);
        verify(mockFriendship).updateRoutableStatus(myId, false);

        verify(friendshipRepository).save(mockFriendship);
    }
    @Test
    @DisplayName("친구 관계 해제 시 복합 ID로 조회 후 검증 및 삭제하며 이벤트를 발행한다")
    void brokeUpWith_Success() {
        // given
        Long myId = 1L;
        Long friendId = 2L;
        String compositeId = Friendship.generateCompositeId(myId, friendId);

        Friendship mockFriendship = mock(Friendship.class);
        SocialUser u1 = new SocialUser(1L, "u1", "");
        SocialUser u2 = new SocialUser(2L, "u2", "");

        given(friendshipRepository.findById(compositeId)).willReturn(Optional.of(mockFriendship));
        given(mockFriendship.getUsers()).willReturn(Set.of(u1, u2));

        // when
        friendshipService.brokeUpWith(myId, friendId);

        // then
        verify(mockFriendship).checkDeletable(myId);
        verify(friendshipRepository).delete(mockFriendship);
        verify(eventPublisher).publishEvent(any(FriendShipDeletedEvent.class));
    }
}