package com.example.GooRoomBe.social.friend.application;

import com.example.GooRoomBe.social.friend.api.dto.FriendUpdateRequestDto;
import com.example.GooRoomBe.social.friend.domain.Friendship;
import com.example.GooRoomBe.social.friend.domain.FriendshipPort;
import com.example.GooRoomBe.social.friend.domain.event.FriendShipDeletedEvent;
import com.example.GooRoomBe.social.socialUser.SocialUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipPort friendshipPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FriendshipService friendshipService;

    private final String MY_ID = "my-id";
    private final String FRIEND_ID = "friend-id";

    @Test
    @DisplayName("updateFriendProps: DTO에 값이 존재하면 해당 속성을 업데이트하고 저장한다")
    void updateFriendProps_ShouldUpdateAndSave() {
        // Given
        FriendUpdateRequestDto dto = new FriendUpdateRequestDto("New Alias", true);
        Friendship mockFriendship = mock(Friendship.class);

        given(friendshipPort.getFriendship(MY_ID, FRIEND_ID)).willReturn(mockFriendship);

        // When
        friendshipService.updateFriendProps(MY_ID, FRIEND_ID, dto);

        // Then
        // 1. 도메인 메서드 호출 검증
        verify(mockFriendship).updateFriendAlias(MY_ID, "New Alias");
        verify(mockFriendship).updateOnIntroduce(MY_ID, true);

        // 2. 저장 호출 검증
        verify(friendshipPort).save(mockFriendship);
    }

    @Test
    @DisplayName("updateFriendProps: DTO 필드가 null이면 해당 업데이트는 건너뛴다")
    void updateFriendProps_ShouldIgnoreNullFields() {
        // Given
        FriendUpdateRequestDto dto = new FriendUpdateRequestDto(null, null); // 값 없음
        Friendship mockFriendship = mock(Friendship.class);

        given(friendshipPort.getFriendship(MY_ID, FRIEND_ID)).willReturn(mockFriendship);

        // When
        friendshipService.updateFriendProps(MY_ID, FRIEND_ID, dto);

        // Then
        // 1. 도메인 메서드가 호출되지 않아야 함
        verify(mockFriendship, never()).updateFriendAlias(any(), any());
        verify(mockFriendship, never()).updateOnIntroduce(any(), anyBoolean());

        // 2. 저장은 여전히 호출됨 (로직상 호출하게 되어 있음)
        verify(friendshipPort).save(mockFriendship);
    }

    @Test
    @DisplayName("deleteFriendShip: 삭제 가능 여부를 확인하고 삭제 후 이벤트를 발행한다")
    void deleteFriendShip_ShouldDeleteAndPublishEvent() {
        // Given
        Friendship mockFriendship = mock(Friendship.class);
        given(friendshipPort.getFriendship(MY_ID, FRIEND_ID)).willReturn(mockFriendship);

        // 이벤트 발행을 위해 User Set 모킹
        SocialUser user1 = mock(SocialUser.class);
        SocialUser user2 = mock(SocialUser.class);
        given(user1.getId()).willReturn(MY_ID);
        given(user2.getId()).willReturn(FRIEND_ID);
        given(mockFriendship.getUsers()).willReturn(Set.of(user1, user2));

        // When
        friendshipService.deleteFriendShip(MY_ID, FRIEND_ID);

        // Then
        verify(mockFriendship).checkDeletable(MY_ID); // 권한 체크 확인
        verify(friendshipPort).delete(mockFriendship); // 삭제 수행 확인
        verify(eventPublisher).publishEvent(any(FriendShipDeletedEvent.class)); // 이벤트 발행 확인
    }
}