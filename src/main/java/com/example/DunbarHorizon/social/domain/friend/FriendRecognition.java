package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.InvalidFriendAliasException;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FriendRecognition {

    public static final double CONVERGENCE_K = 50.0;

    public static final double INITIAL_RAW_SCORE = 21.4;

    private final UserReference user;

    private String friendAlias;

    private Double interestScore = 0.0;

    private LocalDateTime lastInteractedAt;

    private boolean isMuted = false;

    private boolean isRoutable = true;

    public FriendRecognition(UserReference user) {
        this.user = user;
        this.friendAlias = null;
        this.interestScore = INITIAL_RAW_SCORE;
        this.lastInteractedAt = LocalDateTime.now();
    }

    private FriendRecognition(UserReference user, String friendAlias, Double interestScore,
                              LocalDateTime lastInteractedAt, boolean isMuted, boolean isRoutable) {
        this.user = user;
        this.friendAlias = friendAlias;
        this.interestScore = interestScore;
        this.lastInteractedAt = lastInteractedAt;
        this.isMuted = isMuted;
        this.isRoutable = isRoutable;
    }

    public static FriendRecognition restore(UserReference user, String friendAlias, Double interestScore,
                                            LocalDateTime lastInteractedAt, boolean isMuted, boolean isRoutable) {
        return new FriendRecognition(user, friendAlias, interestScore, lastInteractedAt, isMuted, isRoutable);
    }

    public static double normalize(Double raw) {
        if (raw == null || raw <= 0) return 0.0;
        return raw / (raw + CONVERGENCE_K);
    }

    public double getNormalizedScore() {
        return normalize(getInterestScore());
    }

    public void adjustInterestScore(double delta) {
        if (this.interestScore == null) {
            this.interestScore = 0.0;
        }

        this.interestScore += delta;
        this.lastInteractedAt = LocalDateTime.now();

        if (this.interestScore < 0.0) {
            this.interestScore = 0.0;
        }
    }

    boolean isDecayTarget(LocalDateTime thresholdTime) {
        return lastInteractedAt != null && !lastInteractedAt.isAfter(thresholdTime);
    }

    void decay(double rate, double minThreshold, LocalDateTime thresholdTime) {
        double score = getInterestScore();
        if (isDecayTarget(thresholdTime) && score > minThreshold) {
            score = Math.max(score * rate, minThreshold);
        }
        this.interestScore = score;
    }

    public void updateFriendAlias(String newAlias) {
        if (newAlias != null && newAlias.length() > Friendship.ALIAS_MAX_LENGTH) {
            throw new InvalidFriendAliasException();
        }
        this.friendAlias = newAlias;
    }

    public String getFriendAlias() {
        return friendAlias == null ? "" : friendAlias;
    }

    public Double getInterestScore() {
        return this.interestScore == null ? 0.0 : this.interestScore;
    }

    public void updateMuteStatus(boolean isMuted) {
        this.isMuted = isMuted;
    }

    public void updateRoutableStatus(boolean isRoutable) {
        this.isRoutable = isRoutable;
    }
}
