package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RelationshipProperties
@Getter
public class FriendRecognition {

    public static final double CONVERGENCE_K = 50.0;

    public static final double INITIAL_RAW_SCORE = 5.5;

    @Id @GeneratedValue()
    private String id;

    @TargetNode
    private UserReference user;

    private String friendAlias;

    private Double interestScore = 0.0;

    private LocalDateTime lastInteractedAt;

    private boolean isMuted = false;

    private boolean isRoutable = true;

    public FriendRecognition(UserReference user) {
        this.user = user;
        this.friendAlias = null;
        this.interestScore = 0.0;
        this.lastInteractedAt = LocalDateTime.now();
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

    public void updateFriendAlias(String newAlias) {
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
        if (isMuted) {
            updateRoutableStatus(false);
        }
    }

    public void updateRoutableStatus(boolean isRoutable) {
        this.isRoutable = isRoutable;
    }
}