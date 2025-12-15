package com.example.GooRoomBe.social.friend.domain;

import com.example.GooRoomBe.social.friend.exception.FriendshipAuthorizationException;
import com.example.GooRoomBe.social.friend.exception.FriendshipNotFoundException;
import com.example.GooRoomBe.social.socialUser.SocialUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipTest {

    @Mock private SocialUser userA;
    @Mock private SocialUser userB;
    @Mock private SocialUser stranger;

    private Friendship friendship;
    private final String ID_A = "user-2"; // 사전순 뒤
    private final String ID_B = "user-1"; // 사전순 앞 (Composite ID: user-1_user-2)

    @BeforeEach
    void setUp() {
        // ID Stubbing은 setUp에서 한 번만
        when(userA.getId()).thenReturn(ID_A);
        when(userB.getId()).thenReturn(ID_B);

        friendship = new Friendship(userA, userB);
    }

    @Test
    @DisplayName("생성 시 두 유저의 ID를 정렬하여 Composite ID를 만든다")
    void constructor_ShouldGenerateSortedCompositeId() {
        assertThat(friendship.getId()).isEqualTo("user-1_user-2");
        assertThat(friendship.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getFriend: 내 ID를 주면 상대방(친구) 유저 객체를 반환한다")
    void getFriend_ShouldReturnTheOtherUser() {
        assertThat(friendship.getFriend(ID_A)).isEqualTo(userB);
        assertThat(friendship.getFriend(ID_B)).isEqualTo(userA);
    }

    @Test
    @DisplayName("updateAlias: 내 인식(Recognition)의 별명만 수정되어야 한다")
    void updateAlias_ShouldUpdateFriendOnlyMyRecognition() {
        String newAlias = "Best Friend";
        friendship.updateFriendAlias(ID_A, newAlias);

        assertThat(friendship.getFriendAlias(ID_A)).isEqualTo(newAlias);
        assertThat(friendship.getFriendAlias(ID_B)).isEmpty();
    }

    @Test
    @DisplayName("checkDeletable: 참여자는 통과하고 제3자는 예외가 발생한다")
    void checkDeletable_AuthorizationTest() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> friendship.checkDeletable(ID_A));

        when(stranger.getId()).thenReturn("user-stranger");
        assertThatThrownBy(() -> friendship.checkDeletable(stranger.getId()))
                .isInstanceOf(FriendshipAuthorizationException.class);
    }

    @Test
    @DisplayName("관계에 없는 유저 ID로 조회 시 예외가 발생한다")
    void invalidUser_ShouldThrowException() {
        String invalidId = "unknown-user";
        assertThatThrownBy(() -> friendship.getFriend(invalidId))
                .isInstanceOf(FriendshipAuthorizationException.class);
    }


    @Test
    @DisplayName("초기 친밀도는 0이다")
    void intimacy_InitialState() {
        assertThat(friendship.getIntimacy()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("짝사랑(One-sided): 한쪽만 점수가 높으면 친밀도는 0이다 (기하 평균 특성)")
    void intimacy_UnrequitedLove_ShouldBeZero() {
        // When: A가 B에게 100점 관심
        friendship.adjustInterestScore(ID_A, 100.0);

        // Then: B는 0점이므로 Sqrt(100 * 0) = 0
        assertThat(friendship.getIntimacy()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("상호작용(Mutual): 서로 점수가 있어야 친밀도가 상승한다")
    void intimacy_MutualLove_ShouldIncrease() {
        // Given: A가 이미 100점
        friendship.adjustInterestScore(ID_A, 100.0);

        // When: B도 A에게 4점 관심
        friendship.adjustInterestScore(ID_B, 4.0);

        // Then: Sqrt(100 * 4) = 20
        assertThat(friendship.getIntimacy()).isCloseTo(20.0, within(0.001));
    }

    @Test
    @DisplayName("점수 조정 시 친밀도도 즉시 재계산된다")
    void adjustScore_RecalculatesIntimacy() {
        // Given: 둘 다 100점 (Intimacy = 100)
        friendship.adjustInterestScore(ID_A, 100.0);
        friendship.adjustInterestScore(ID_B, 100.0);
        assertThat(friendship.getIntimacy()).isEqualTo(100.0);

        // When: A의 점수를 50점 깎음 (100 -> 50)
        friendship.adjustInterestScore(ID_A, -50.0);

        // Then: Sqrt(50 * 100) = Sqrt(5000) ≈ 70.71
        assertThat(friendship.getIntimacy()).isCloseTo(70.710, within(0.001));
    }
}