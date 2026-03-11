package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

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
    @DisplayName("관심도 점수에 따라 친밀도가 정규화된 값의 기하평균으로 정확히 계산되어야 한다")
    void recalculateIntimacy_Success() {
        // given
        SocialUser userA = new SocialUser(1L, "A", "");
        SocialUser userB = new SocialUser(2L, "B", "");
        Friendship friendship = new Friendship(userA, userB);

        double rawA = 4.0;
        double rawB = 9.0;
        double k = FriendRecognition.CONVERGENCE_K; // 50.0

        // when
        friendship.adjustInterestScore(1L, rawA);
        friendship.adjustInterestScore(2L, rawB);

        // then
        // 정규화 점수 계산: raw / (raw + 50)
        double normA = rawA / (rawA + k); // 4 / 54 ≈ 0.07407
        double normB = rawB / (rawB + k); // 9 / 59 ≈ 0.15254
        double expectedIntimacy = Math.sqrt(normA * normB); // sqrt(0.07407 * 0.15254) ≈ 0.1063

        assertThat(friendship.getIntimacy())
                .isCloseTo(expectedIntimacy, within(0.0001));
    }

    @Test
    @DisplayName("한 명의 관심도가 0이라면 정규화 후에도 친밀도는 0이어야 한다")
    void recalculateIntimacy_WithZero() {
        // given
        Friendship friendship = new Friendship(new SocialUser(1L, "A", ""), new SocialUser(2L, "B", ""));

        // when
        friendship.adjustInterestScore(1L, 100.0);
        friendship.adjustInterestScore(2L, 0.0); // 0 / (0 + 50) = 0

        // then
        assertThat(friendship.getIntimacy()).isEqualTo(0.0);
    }
}