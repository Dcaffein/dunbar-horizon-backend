package com.example.DunbarHorizon.social.domain.friendship;

import com.example.DunbarHorizon.social.domain.friendship.exception.InvalidFriendAliasException;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendshipInvalidException;
import com.example.DunbarHorizon.social.domain.friendship.exception.FriendshipAuthorizationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

class FriendshipTest {

    @Test
    @DisplayName("역순으로 생성해도 작은 ID의 인식을 lower에 배치한다")
    void create_OrdersRecognitions_ReversedIds() {
        // when
        Friendship friendship = new Friendship(2L, 1L);

        // then
        assertThat(friendship.getLower().getOwnerId()).isEqualTo(1L);
        assertThat(friendship.getHigher().getOwnerId()).isEqualTo(2L);
        assertThat(friendship.getFriendId(1L)).isEqualTo(2L);
        assertThat(friendship.getFriendId(2L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 사용자 또는 null 사용자로 친구 관계를 생성하지 못한다")
    void create_Fails_InvalidUserIds() {
        assertThatThrownBy(() -> new Friendship(1L, 1L)).isInstanceOf(FriendshipInvalidException.class);
        assertThatThrownBy(() -> new Friendship(null, 1L)).isInstanceOf(FriendshipInvalidException.class);
        assertThatThrownBy(() -> new Friendship(1L, null)).isInstanceOf(FriendshipInvalidException.class);
    }

    @Test
    @DisplayName("복원 시 입력 순서를 정규화하면서 사용자별 설정을 보존하고 친밀도는 두 관심도로 계산한다")
    void restore_PreservesState_ReversedRecognitions() {
        // given
        FriendRecognition high = FriendRecognition.restore(2L, "high alias", 40.0, RECENT, true, false);
        FriendRecognition low = FriendRecognition.restore(1L, "low alias", 20.0, STALE, false, true);

        // when
        Friendship friendship = Friendship.restore(LocalDate.of(2026, 1, 1), high, low);

        // then
        assertThat(friendship.getLower()).isSameAs(low);
        assertThat(friendship.getHigher()).isSameAs(high);
        assertThat(friendship.getFriendAlias(1L)).isEqualTo("low alias");
        assertThat(friendship.getFriendAlias(2L)).isEqualTo("high alias");
        assertThat(friendship.getIntimacy()).isCloseTo(intimacyOf(20.0, 40.0), within(1e-12));
    }

    @Test
    @DisplayName("복원 시 인식 누락과 중복 사용자 및 null 사용자 ID를 거절한다")
    void restore_Fails_InvalidRecognitions() {
        // given
        FriendRecognition one = new FriendRecognition(1L);
        LocalDate date = LocalDate.of(2026, 1, 1);

        // when & then
        assertThatThrownBy(() -> Friendship.restore(date, null, one))
                .isInstanceOf(FriendshipInvalidException.class);
        assertThatThrownBy(() -> Friendship.restore(date, one, null))
                .isInstanceOf(FriendshipInvalidException.class);
        assertThatThrownBy(() -> Friendship.restore(date, one, new FriendRecognition(1L)))
                .isInstanceOf(FriendshipInvalidException.class);
        assertThatThrownBy(() -> Friendship.restore(date, one, new FriendRecognition(null)))
                .isInstanceOf(FriendshipInvalidException.class);
    }

    @Test
    @DisplayName("관계의 구성원이 아닌 사용자는 조회하거나 상태를 변경할 수 없다")
    void access_Fails_NonMember() {
        // given
        Friendship friendship = new Friendship(1L, 2L);
        double intimacy = friendship.getIntimacy();

        // when & then
        assertThatThrownBy(() -> friendship.getFriendId(3L)).isInstanceOf(FriendshipAuthorizationException.class);
        assertThatThrownBy(() -> friendship.getFriendAlias(3L)).isInstanceOf(FriendshipAuthorizationException.class);
        assertThatThrownBy(() -> friendship.adjustInterestScore(3L, 10)).isInstanceOf(FriendshipAuthorizationException.class);
        assertThatThrownBy(() -> friendship.updateSettings(3L, "alias", true, false))
                .isInstanceOf(FriendshipAuthorizationException.class);
        assertThat(friendship.getIntimacy()).isEqualTo(intimacy);
    }

    @Test
    @DisplayName("높은 ID 사용자의 설정 변경은 낮은 ID 사용자의 설정과 점수를 바꾸지 않는다")
    void updateSettings_PreservesOtherSide_HigherUser() {
        // given
        Friendship friendship = new Friendship(2L, 1L);
        double intimacy = friendship.getIntimacy();

        // when
        friendship.updateSettings(2L, "alias", true, false);

        // then
        assertThat(friendship.getFriendAlias(2L)).isEqualTo("alias");
        assertThat(friendship.isMuted(2L)).isTrue();
        assertThat(friendship.isRoutable(2L)).isFalse();
        assertThat(friendship.getFriendAlias(1L)).isEmpty();
        assertThat(friendship.isMuted(1L)).isFalse();
        assertThat(friendship.isRoutable(1L)).isTrue();
        assertThat(friendship.getIntimacy()).isEqualTo(intimacy);
        assertThat(friendship.getMyInterestScore(1L)).isEqualTo(FriendRecognition.INITIAL_RAW_SCORE);
        assertThat(friendship.getMyInterestScore(2L)).isEqualTo(FriendRecognition.INITIAL_RAW_SCORE);
    }

    @Test
    @DisplayName("관심도 점수에 따라 친밀도가 정규화된 값의 기하평균으로 정확히 계산되어야 한다")
    void recalculateIntimacy_Success() {
        // given
        Long userA = 1L;
        Long userB = 2L;
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
        Long userA = 1L;
        Long userB = 2L;
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
        Friendship friendship = new Friendship(1L, 2L);

        // when
        friendship.adjustInterestScore(1L, 100.0);
        friendship.adjustInterestScore(2L, -FriendRecognition.INITIAL_RAW_SCORE); // INITIAL_RAW_SCORE - INITIAL_RAW_SCORE = 0

        // then
        assertThat(friendship.getIntimacy()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("별명에 비어있지 않은 값을 전달하면 해당 값으로 설정된다")
    void updateSettings_withAlias_setsAlias() {
        // given
        Friendship friendship = new Friendship(1L, 2L);

        // when
        friendship.updateSettings(1L, "동기", null, null);

        // then
        assertThat(friendship.getFriendAlias(1L)).isEqualTo("동기");
    }

    @Test
    @DisplayName("별명에 빈 문자열을 전달하면 alias가 삭제되어 빈 문자열로 반환된다")
    void updateSettings_withEmptyAlias_clearsAlias() {
        // given
        Friendship friendship = new Friendship(1L, 2L);
        friendship.updateSettings(1L, "동기", null, null);

        // when
        friendship.updateSettings(1L, "", null, null);

        // then
        assertThat(friendship.getFriendAlias(1L)).isEqualTo(""); // null 저장 → getFriendAlias는 "" 반환
    }

    @Test
    @DisplayName("별명에 null을 전달하면 기존 alias가 변경되지 않는다")
    void updateSettings_withNullAlias_doesNotChangeAlias() {
        // given
        Friendship friendship = new Friendship(1L, 2L);
        friendship.updateSettings(1L, "동기", null, null);

        // when
        friendship.updateSettings(1L, null, null, null);

        // then
        assertThat(friendship.getFriendAlias(1L)).isEqualTo("동기");
    }

    @Test
    @DisplayName("별명이 20자를 초과하면 InvalidFriendAliasException이 발생한다")
    void updateSettings_withTooLongAlias_throws() {
        Friendship friendship = new Friendship(1L, 2L);
        String tooLong = "가".repeat(Friendship.ALIAS_MAX_LENGTH + 1);

        assertThatThrownBy(() -> friendship.updateSettings(1L, tooLong, null, null))
                .isInstanceOf(InvalidFriendAliasException.class);
    }

    private static final LocalDateTime DECAY_TIME = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime STALE = DECAY_TIME.minusDays(10);
    private static final LocalDateTime RECENT = DECAY_TIME.plusDays(10);
    private static final double RATE = 0.5;
    private static final double THRESHOLD = 1.0;

    private static Friendship friendshipOf(double lowScore, LocalDateTime lowAt, double highScore, LocalDateTime highAt) {
        FriendRecognition low = FriendRecognition.restore(1L, null, lowScore, lowAt, false, true);
        FriendRecognition high = FriendRecognition.restore(2L, null, highScore, highAt, false, true);
        return Friendship.restore(LocalDate.of(2026, 1, 1), low, high);
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
    @DisplayName("이미 하한 이하인 관심도는 바뀌지 않고 친밀도도 그대로다")
    void decay_alreadyAtOrBelowThreshold_keepsScoreAndIntimacy() {
        // given
        Friendship friendship = Friendship.restore(LocalDate.of(2026, 1, 1),
                FriendRecognition.restore(1L, null, 0.5, STALE, false, true),
                FriendRecognition.restore(2L, null, THRESHOLD, STALE, false, true));

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
        Friendship friendship = Friendship.restore(LocalDate.of(2026, 1, 1),
                FriendRecognition.restore(1L, null, 40.0, RECENT, false, true),
                FriendRecognition.restore(2L, null, 20.0, RECENT, false, true));

        // when
        friendship.decay(RATE, THRESHOLD, DECAY_TIME);

        // then
        assertThat(friendship.getMyInterestScore(1L)).isEqualTo(40.0);
        assertThat(friendship.getMyInterestScore(2L)).isEqualTo(20.0);
        assertThat(friendship.getIntimacy()).isCloseTo(intimacyOf(40.0, 20.0), within(1e-12));
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
        assertThat(friendship.getLower().getLastInteractedAt()).isEqualTo(STALE);
        assertThat(friendship.getHigher().getLastInteractedAt()).isEqualTo(RECENT);
    }

    @Test
    @DisplayName("감쇠는 관심도를 깎고 친밀도는 깎인 관심도에서 다시 계산된다")
    void decay_LowersIntimacy_ThroughInterestScores() {
        // given
        Friendship friendship = friendshipOf(40.0, STALE, 20.0, STALE);
        double before = friendship.getIntimacy();

        // when
        friendship.decay(RATE, THRESHOLD, DECAY_TIME);

        // then
        assertThat(friendship.getIntimacy()).isLessThan(before);
        assertThat(friendship.getIntimacy()).isEqualTo(intimacyOf(friendship.getMyInterestScore(1L),
                friendship.getMyInterestScore(2L)));
    }
}
