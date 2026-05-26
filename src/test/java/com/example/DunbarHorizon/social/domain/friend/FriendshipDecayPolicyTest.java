package com.example.DunbarHorizon.social.domain.friend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
class FriendshipDecayPolicyTest {

    @InjectMocks
    private FriendshipDecayPolicy policy;

    @Test
    @DisplayName("DECAY_FROM=21.4, ACTUAL_DECAY_DAYS=90 기준 rate가 올바르게 계산된다")
    void getDecayRate_사용값_검증() {
        // given
        double decayFrom = 21.4;
        double referenceScore = 1.0;
        int decayDays = 90;

        // when
        double rate = policy.getDecayRate();

        // then: rate = (1.0 / 21.4)^(1/90)
        double expected = Math.pow(referenceScore / decayFrom, 1.0 / decayDays);
        assertThat(rate).isCloseTo(expected, within(1e-10));

        // 90일 적용 시 21.4점 → 1.0점에 도달하는지 검증
        double result = decayFrom * Math.pow(rate, decayDays);
        assertThat(result).isCloseTo(referenceScore, within(1e-6));
    }

    @Test
    @DisplayName("MIN_RAW_THRESHOLD는 1.0이다")
    void getMinThreshold_반환값_검증() {
        assertThat(policy.getMinThreshold()).isEqualTo(1.0);
    }
}
