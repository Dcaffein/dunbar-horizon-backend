package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.InvalidFriendAliasException;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        double deltaA = 4.0;
        double deltaB = 9.0;
        double k = FriendRecognition.CONVERGENCE_K; // 50.0

        // when
        friendship.adjustInterestScore(1L, deltaA);
        friendship.adjustInterestScore(2L, deltaB);

        // then
        // adjustInterestScore는 delta를 더하므로 최종 score = INITIAL_RAW_SCORE + delta
        double scoreA = FriendRecognition.INITIAL_RAW_SCORE + deltaA;
        double scoreB = FriendRecognition.INITIAL_RAW_SCORE + deltaB;
        double normA = scoreA / (scoreA + k);
        double normB = scoreB / (scoreB + k);
        double expectedIntimacy = Math.sqrt(normA * normB);

        assertThat(friendship.getIntimacy())
                .isCloseTo(expectedIntimacy, within(0.0001));
    }

    @Test
    @DisplayName("adjustMutualInterestScore는 양쪽 모두 동일한 delta를 적용하고 intimacy를 1회 재계산한다")
    void adjustMutualInterestScore_updatesBothSides() {
        // given
        SocialUser userA = new SocialUser(1L, "A", "");
        SocialUser userB = new SocialUser(2L, "B", "");
        Friendship friendship = new Friendship(userA, userB);
        double delta = 10.0;
        double k = FriendRecognition.CONVERGENCE_K;
        double initialScore = FriendRecognition.INITIAL_RAW_SCORE;

        // when
        friendship.adjustMutualInterestScore(delta);

        // then
        double expectedScore = initialScore + delta;
        assertThat(friendship.getMyInterestScore(1L)).isCloseTo(expectedScore, within(0.0001));
        assertThat(friendship.getMyInterestScore(2L)).isCloseTo(expectedScore, within(0.0001));

        double norm = expectedScore / (expectedScore + k);
        double expectedIntimacy = Math.sqrt(norm * norm);
        assertThat(friendship.getIntimacy()).isCloseTo(expectedIntimacy, within(0.0001));
    }

    @Test
    @DisplayName("한 명의 관심도가 0이라면 정규화 후에도 친밀도는 0이어야 한다")
    void recalculateIntimacy_WithZero() {
        // given
        Friendship friendship = new Friendship(new SocialUser(1L, "A", ""), new SocialUser(2L, "B", ""));

        // when
        friendship.adjustInterestScore(1L, 100.0);
        friendship.adjustInterestScore(2L, -FriendRecognition.INITIAL_RAW_SCORE); // INITIAL_RAW_SCORE - INITIAL_RAW_SCORE = 0

        // then
        assertThat(friendship.getIntimacy()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("별명에 비어있지 않은 값을 전달하면 해당 값으로 설정된다")
    void updateUserFields_withAlias_setsAlias() {
        // given
        Friendship friendship = new Friendship(new SocialUser(1L, "A", ""), new SocialUser(2L, "B", ""));

        // when
        friendship.updateUserFields(1L, "동기", null, null);

        // then
        assertThat(friendship.getFriendAlias(1L)).isEqualTo("동기");
    }

    @Test
    @DisplayName("별명에 빈 문자열을 전달하면 alias가 삭제되어 빈 문자열로 반환된다")
    void updateUserFields_withEmptyAlias_clearsAlias() {
        // given
        Friendship friendship = new Friendship(new SocialUser(1L, "A", ""), new SocialUser(2L, "B", ""));
        friendship.updateUserFields(1L, "동기", null, null);

        // when
        friendship.updateUserFields(1L, "", null, null);

        // then
        assertThat(friendship.getFriendAlias(1L)).isEqualTo(""); // null 저장 → getFriendAlias는 "" 반환
    }

    @Test
    @DisplayName("별명에 null을 전달하면 기존 alias가 변경되지 않는다")
    void updateUserFields_withNullAlias_doesNotChangeAlias() {
        // given
        Friendship friendship = new Friendship(new SocialUser(1L, "A", ""), new SocialUser(2L, "B", ""));
        friendship.updateUserFields(1L, "동기", null, null);

        // when
        friendship.updateUserFields(1L, null, null, null);

        // then
        assertThat(friendship.getFriendAlias(1L)).isEqualTo("동기");
    }

    @Test
    @DisplayName("별명이 20자를 초과하면 InvalidFriendAliasException이 발생한다")
    void updateUserFields_withTooLongAlias_throws() {
        Friendship friendship = new Friendship(new SocialUser(1L, "A", ""), new SocialUser(2L, "B", ""));
        String tooLong = "가".repeat(Friendship.ALIAS_MAX_LENGTH + 1);

        assertThatThrownBy(() -> friendship.updateUserFields(1L, tooLong, null, null))
                .isInstanceOf(InvalidFriendAliasException.class);
    }

    private static final LocalDateTime DECAY_TIME = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime STALE = DECAY_TIME.minusDays(10);
    private static final LocalDateTime RECENT = DECAY_TIME.plusDays(10);
    private static final double RATE = 0.5;
    private static final double THRESHOLD = 1.0;

    private static Friendship friendshipOf(double lowScore, LocalDateTime lowAt, double highScore, LocalDateTime highAt) {
        FriendRecognition low = FriendRecognition.restore(new SocialUser(1L, "A", ""), null, lowScore, lowAt, false, true);
        FriendRecognition high = FriendRecognition.restore(new SocialUser(2L, "B", ""), null, highScore, highAt, false, true);
        double intimacy = Math.sqrt(FriendRecognition.normalize(lowScore) * FriendRecognition.normalize(highScore));
        return Friendship.restore("1_2", LocalDate.of(2026, 1, 1), intimacy, List.of(low, high));
    }

    private static double intimacyOf(double a, double b) {
        return Math.sqrt(FriendRecognition.normalize(a) * FriendRecognition.normalize(b));
    }

    @Test
    @DisplayName("양쪽 모두 마지막 교류가 기준 시각 이전이면 양쪽 관심도를 감쇄하고 친밀도를 다시 계산한다")
    void decay_bothStale_decaysBothSides() {
        // given
        Friendship friendship = friendshipOf(40.0, STALE, 20.0, STALE);

        // when
        friendship.decay(RATE, THRESHOLD, DECAY_TIME);

        // then
        assertThat(friendship.getMyInterestScore(1L)).isEqualTo(20.0);
        assertThat(friendship.getMyInterestScore(2L)).isEqualTo(10.0);
        assertThat(friendship.getIntimacy()).isCloseTo(intimacyOf(20.0, 10.0), within(1e-12));
    }

    @Test
    @DisplayName("한쪽만 기준 시각 이전이면 그쪽만 감쇄하고 친밀도는 양쪽 점수로 다시 계산한다")
    void decay_oneSideStale_decaysOnlyThatSide() {
        // given
        Friendship friendship = friendshipOf(40.0, STALE, 20.0, RECENT);

        // when
        friendship.decay(RATE, THRESHOLD, DECAY_TIME);

        // then
        assertThat(friendship.getMyInterestScore(1L)).isEqualTo(20.0);
        assertThat(friendship.getMyInterestScore(2L)).isEqualTo(20.0);
        assertThat(friendship.getIntimacy()).isCloseTo(intimacyOf(20.0, 20.0), within(1e-12));
    }

    @Test
    @DisplayName("마지막 교류가 기준 시각과 같으면 감쇄 대상이다")
    void decay_atThresholdTime_isTarget() {
        // given
        Friendship friendship = friendshipOf(40.0, DECAY_TIME, 20.0, RECENT);

        // when
        friendship.decay(RATE, THRESHOLD, DECAY_TIME);

        // then
        assertThat(friendship.getMyInterestScore(1L)).isEqualTo(20.0);
    }

    @Test
    @DisplayName("감쇄하면 하한 아래로 떨어지는 관심도는 하한에서 멈춘다")
    void decay_belowThreshold_clampsToThreshold() {
        // given
        Friendship friendship = friendshipOf(1.5, STALE, 20.0, STALE);

        // when
        friendship.decay(RATE, THRESHOLD, DECAY_TIME);

        // then
        assertThat(friendship.getMyInterestScore(1L)).isEqualTo(THRESHOLD);
        assertThat(friendship.getIntimacy()).isCloseTo(intimacyOf(THRESHOLD, 10.0), within(1e-12));
    }

    @Test
    @DisplayName("이미 하한 이하인 관심도는 바뀌지 않지만 대상 관계라 친밀도는 다시 계산한다")
    void decay_alreadyAtOrBelowThreshold_keepsScoreAndRecalculatesIntimacy() {
        // given
        Friendship friendship = Friendship.restore("1_2", LocalDate.of(2026, 1, 1), 0.9, List.of(
                FriendRecognition.restore(new SocialUser(1L, "A", ""), null, 0.5, STALE, false, true),
                FriendRecognition.restore(new SocialUser(2L, "B", ""), null, THRESHOLD, STALE, false, true)));

        // when
        friendship.decay(RATE, THRESHOLD, DECAY_TIME);

        // then
        assertThat(friendship.getMyInterestScore(1L)).isEqualTo(0.5);
        assertThat(friendship.getMyInterestScore(2L)).isEqualTo(THRESHOLD);
        assertThat(friendship.getIntimacy()).isCloseTo(intimacyOf(0.5, THRESHOLD), within(1e-12));
    }

    @Test
    @DisplayName("양쪽 모두 최근에 교류했으면 관심도와 친밀도가 바뀌지 않는다")
    void decay_bothRecent_changesNothing() {
        // given
        Friendship friendship = Friendship.restore("1_2", LocalDate.of(2026, 1, 1), 0.9, List.of(
                FriendRecognition.restore(new SocialUser(1L, "A", ""), null, 40.0, RECENT, false, true),
                FriendRecognition.restore(new SocialUser(2L, "B", ""), null, 20.0, RECENT, false, true)));

        // when
        friendship.decay(RATE, THRESHOLD, DECAY_TIME);

        // then
        assertThat(friendship.getMyInterestScore(1L)).isEqualTo(40.0);
        assertThat(friendship.getMyInterestScore(2L)).isEqualTo(20.0);
        assertThat(friendship.getIntimacy()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("마지막 교류 시각이 없는 쪽은 감쇄 대상이 아니다")
    void decay_nullLastInteractedAt_isNotTarget() {
        // given
        Friendship friendship = friendshipOf(40.0, null, 20.0, RECENT);

        // when
        friendship.decay(RATE, THRESHOLD, DECAY_TIME);

        // then
        assertThat(friendship.getMyInterestScore(1L)).isEqualTo(40.0);
    }

    @Test
    @DisplayName("감쇄는 마지막 교류 시각을 바꾸지 않는다")
    void decay_keepsLastInteractedAt() {
        // given
        Friendship friendship = friendshipOf(40.0, STALE, 20.0, RECENT);

        // when
        friendship.decay(RATE, THRESHOLD, DECAY_TIME);

        // then
        assertThat(friendship.getRecognitions().get(0).getLastInteractedAt()).isEqualTo(STALE);
        assertThat(friendship.getRecognitions().get(1).getLastInteractedAt()).isEqualTo(RECENT);
    }
}