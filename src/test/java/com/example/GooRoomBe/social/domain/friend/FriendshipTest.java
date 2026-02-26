package com.example.GooRoomBe.social.domain.friend;

import com.example.GooRoomBe.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class FriendshipTest {

    @Test
    @DisplayName("유저 ID 순서에 상관없이 항상 동일한 복합 ID가 생성되어야 한다")
    void generateCompositeId_Consistency() {
        // given
        SocialUser user1 = new SocialUser(1L, "유저1", "");
        SocialUser user2 = new SocialUser(2L, "유저2", "");

        // when
        Friendship friendship1 = new Friendship(user1, user2);
        Friendship friendship2 = new Friendship(user2, user1);

        // then
        assertThat(friendship1.getId()).isEqualTo("1_2");
        assertThat(friendship2.getId()).isEqualTo("1_2");
    }

    @Test
    @DisplayName("관심도 점수에 따라 친밀도가 기하평균(sqrt(A*B))으로 정확히 계산되어야 한다")
    void recalculateIntimacy_Success() {
        // given
        SocialUser userA = new SocialUser(1L, "A", "");
        SocialUser userB = new SocialUser(2L, "B", "");
        Friendship friendship = new Friendship(userA, userB);

        // when: A의 점수 4.0, B의 점수 9.0 주입
        friendship.adjustInterestScore(1L, 4.0);
        friendship.adjustInterestScore(2L, 9.0);

        // then: sqrt(4 * 9) = 6.0
        assertThat(friendship.getIntimacy()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("한 명의 관심도가 0이라면 친밀도는 항상 0이어야 한다")
    void recalculateIntimacy_WithZero() {
        // given
        Friendship friendship = new Friendship(new SocialUser(1L, "A", ""), new SocialUser(2L, "B", ""));

        // when
        friendship.adjustInterestScore(1L, 100.0);
        friendship.adjustInterestScore(2L, 0.0);

        // then: sqrt(100 * 0) = 0
        assertThat(friendship.getIntimacy()).isEqualTo(0.0);
    }
}