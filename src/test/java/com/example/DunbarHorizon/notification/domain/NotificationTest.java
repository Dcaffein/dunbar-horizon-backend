package com.example.DunbarHorizon.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    @DisplayName("보관 기준선은 기준 시각에서 30일 전이다")
    void retentionCutoff_Success_ThirtyDaysBeforeAsOf() {
        // given
        LocalDateTime asOf = LocalDateTime.of(2026, 3, 31, 12, 0);

        // when
        LocalDateTime cutoff = Notification.retentionCutoff(asOf);

        // then
        assertThat(cutoff).isEqualTo(LocalDateTime.of(2026, 3, 1, 12, 0));
    }
}
