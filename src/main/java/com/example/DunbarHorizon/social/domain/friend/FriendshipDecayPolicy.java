package com.example.DunbarHorizon.social.domain.friend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FriendshipDecayPolicy {

    // 감쇄 속도 기준점 (INITIAL_RAW_SCORE와 독립적으로 관리)
    private static final double DECAY_FROM = 21.4;

    // ACTUAL_DECAY_DAYS 이후 이 점수까지 떨어지는 속도로 감쇄
    private static final double DECAY_REFERENCE_SCORE = 1.0;

    private static final int GRACE_PERIOD_DAYS = 30;
    private static final int ACTUAL_DECAY_DAYS = 90;

    // 최소 하한선 (raw score)
    private static final double MIN_RAW_THRESHOLD = 1.0;

    // Rate = (DECAY_REFERENCE_SCORE / DECAY_FROM) ^ (1 / ACTUAL_DECAY_DAYS)
    // 예: 21.4점이 90일 뒤에 1.0점이 되려면 매일 약 3.3%씩 감소해야 함 (Rate ≒ 0.967)
    public double getDecayRate() {
        return Math.pow(DECAY_REFERENCE_SCORE / DECAY_FROM, 1.0 / ACTUAL_DECAY_DAYS);
    }

    public double getMinThreshold() {
        return MIN_RAW_THRESHOLD;
    }

    public LocalDateTime getDecayThresholdTime() {
        return LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS);
    }
}
