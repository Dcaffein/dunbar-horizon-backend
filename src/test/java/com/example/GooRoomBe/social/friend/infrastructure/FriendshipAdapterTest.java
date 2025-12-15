package com.example.GooRoomBe.social.friend.infrastructure;

import com.example.GooRoomBe.social.friend.domain.Friendship;
import com.example.GooRoomBe.social.friend.exception.FriendshipNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendshipAdapterTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @InjectMocks
    private FriendshipAdapter friendshipAdapter;

    private final String MY_ID = "my-id";
    private final String FRIEND_ID = "friend-id";

    @Test
    @DisplayName("getFriendship: 관계가 존재하면 Friendship 객체를 반환한다")
    void getFriendship_Success() {
        // Given
        Friendship mockFriendship = mock(Friendship.class);
        given(friendshipRepository.findFriendshipByUsers(MY_ID, FRIEND_ID))
                .willReturn(Optional.of(mockFriendship));

        // When
        Friendship result = friendshipAdapter.getFriendship(MY_ID, FRIEND_ID);

        // Then
        assertThat(result).isEqualTo(mockFriendship);
    }

    @Test
    @DisplayName("getFriendship: 관계가 없으면 FriendshipNotFoundException을 던진다")
    void getFriendship_NotFound() {
        // Given
        given(friendshipRepository.findFriendshipByUsers(MY_ID, FRIEND_ID))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> friendshipAdapter.getFriendship(MY_ID, FRIEND_ID))
                .isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    @DisplayName("applyDecay: 리포지토리의 쿼리 메서드를 올바른 파라미터로 호출한다")
    void applyDecay_Delegation() {
        // When
        friendshipAdapter.applyDecayToAllFriendships(0.95, 0.1);

        // Then
        verify(friendshipRepository).applyDecayToAllFriendships(0.95, 0.1);
    }

    // save, delete 등은 단순 위임이므로 필요하다면 verify로 간단히 작성 가능
    @Test
    void save_Delegation() {
        Friendship friendship = mock(Friendship.class);
        friendshipAdapter.save(friendship);
        verify(friendshipRepository).save(friendship);
    }
}