package com.example.DunbarHorizon.social.domain.friend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FriendshipDecayPolicy {


    private static final double INITIAL_RAW = FriendRecognition.INITIAL_RAW_SCORE;

    // "한 달간 방치 시, 초기 점수가 이 점수까지 떨어지는 속도로 깎겠다"
    private static final double DECAY_REFERENCE_SCORE = 1.0;

    private static final int GRACE_PERIOD_DAYS = 7;
    private static final int ACTUAL_DECAY_DAYS = 30;

    // 최소 하한선
    private static final double MIN_RAW_THRESHOLD = 0.1;

    // Rate = (SCORE / INITIAL_RAW) ^ (1 / DECAY_TARGET_DAYS)
    // * 예: 5.5점이 30일 뒤에 1.0점이 되려면 매일 약 5.5%씩 감소해야 함 (Rate ≒ 0.945)
    public double getDecayRate() {
        return Math.pow(DECAY_REFERENCE_SCORE / INITIAL_RAW, 1.0 / ACTUAL_DECAY_DAYS);
    }

    public double getMinThreshold() {
        return MIN_RAW_THRESHOLD;
    }

    public LocalDateTime getDecayThresholdTime() {
        return LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS);
    }
}