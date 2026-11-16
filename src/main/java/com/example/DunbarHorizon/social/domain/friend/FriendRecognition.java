package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.InvalidFriendAliasException;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FriendRecognition {

    public static final double CONVERGENCE_K = 50.0;

    public static final double INITIAL_RAW_SCORE = 21.4;

    private final Long ownerId;

    private String friendAlias;

    private double interestScore;

    private LocalDateTime lastInteractedAt;

    private boolean isMuted = false;

    private boolean isRoutable = true;

    FriendRecognition(Long ownerId) {
        this.ownerId = ownerId;
        this.friendAlias = null;
        this.interestScore = INITIAL_RAW_SCORE;
        this.lastInteractedAt = LocalDateTime.now();
    }

    private FriendRecognition(Long ownerId, String friendAlias, double interestScore,
                              LocalDateTime lastInteractedAt, boolean isMuted, boolean isRoutable) {
        this.ownerId = ownerId;
        this.friendAlias = friendAlias;
        this.interestScore = interestScore;
        this.lastInteractedAt = lastInteractedAt;
        this.isMuted = isMuted;
        this.isRoutable = isRoutable;
    }

    public static FriendRecognition restore(Long ownerId, String friendAlias, double interestScore,
                                            LocalDateTime lastInteractedAt, boolean isMuted, boolean isRoutable) {
        return new FriendRecognition(ownerId, friendAlias, interestScore, lastInteractedAt, isMuted, isRoutable);
    }

    public static double normalize(double raw) {
        if (raw <= 0) return 0.0;
        return raw / (raw + CONVERGENCE_K);
    }

    public double getNormalizedScore() {
        return normalize(getInterestScore());
    }

    void adjustInterestScore(double delta) {
        this.interestScore = Math.max(0.0, interestScore + delta);
        this.lastInteractedAt = LocalDateTime.now();
    }

    void decay(double rate, double minThreshold, LocalDateTime thresholdTime) {
        if (isDecayTarget(thresholdTime) && interestScore > minThreshold) {
            this.interestScore = Math.max(interestScore * rate, minThreshold);
        }
    }

    private boolean isDecayTarget(LocalDateTime thresholdTime) {
        return lastInteractedAt != null && !lastInteractedAt.isAfter(thresholdTime);
    }

    void updateFriendAlias(String newAlias) {
        validateFriendAlias(newAlias);
        this.friendAlias = newAlias;
    }

    private static void validateFriendAlias(String newAlias) {
        if (newAlias != null && newAlias.length() > Friendship.ALIAS_MAX_LENGTH) {
            throw new InvalidFriendAliasException();
        }
    }

    public String getFriendAlias() {
        return friendAlias == null ? "" : friendAlias;
    }

    void updateMuteStatus(boolean isMuted) {
        this.isMuted = isMuted;
    }

    void updateRoutableStatus(boolean isRoutable) {
        this.isRoutable = isRoutable;
    }
}
