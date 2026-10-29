package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionScorePolicyTest {

    @ParameterizedTest
    @EnumSource(InteractionType.class)
    @DisplayName("모든 상호작용 타입의 점수는 양수다")
    void scoreOf_everyTypeIsPositive(InteractionType type) {
        // when
        double score = InteractionScorePolicy.scoreOf(type);

        // then
        assertThat(score).isPositive();
    }

    @Test
    @DisplayName("보존된 모임은 기본 점수에 그 절반을 얹은 점수를 받는다")
    void scoreOf_preservedCarriesHalfOfBaseAsBonus() {
        // when
        double base = InteractionScorePolicy.scoreOf(InteractionType.FLAG_ENDED);
        double preserved = InteractionScorePolicy.scoreOf(InteractionType.FLAG_PRESERVED);

        // then
        assertThat(preserved).isEqualTo(base * 1.5);
    }

    @Test
    @DisplayName("앵코르에도 같은 비율의 보존 가중치가 붙는다")
    void scoreOf_encorePreservedKeepsTheSameRatio() {
        // when
        double encoreBase = InteractionScorePolicy.scoreOf(InteractionType.FLAG_ENDED_ENCORE);
        double encorePreserved = InteractionScorePolicy.scoreOf(InteractionType.FLAG_PRESERVED_ENCORE);

        // then
        assertThat(encorePreserved).isEqualTo(encoreBase * 1.5);
    }

    @Test
    @DisplayName("보존 가중치는 모임을 한 번 더 한 것보다 약하다")
    void scoreOf_preservationBonusIsWeakerThanAnotherMeeting() {
        // when
        double base = InteractionScorePolicy.scoreOf(InteractionType.FLAG_ENDED);
        double preserved = InteractionScorePolicy.scoreOf(InteractionType.FLAG_PRESERVED);

        // then
        assertThat(preserved).isGreaterThan(base).isLessThan(base * 2);
    }

    @Test
    @DisplayName("회수는 부여와 크기가 같고 부호만 반대다")
    void signed_revokeMirrorsGrant() {
        // given
        double score = InteractionScorePolicy.scoreOf(InteractionType.FLAG_PRESERVED);

        // when
        double granted = ScoreDirection.GRANT.signed(score);
        double revoked = ScoreDirection.REVOKE.signed(score);

        // then
        assertThat(granted).isEqualTo(score);
        assertThat(revoked).isEqualTo(-score);
        assertThat(granted + revoked).isZero();
    }
}
