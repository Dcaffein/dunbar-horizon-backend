package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipAuthorizationException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipInvalidException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@DynamicUpdate
@Table(name = "friendships")
@IdClass(FriendshipId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {

    public static final int ALIAS_MAX_LENGTH = 20;
    public static final String ALIAS_LENGTH_MESSAGE = "별명은 20자 이하로 입력해주세요.";

    @Id
    @Column(name = "low_user_id")
    private Long lowUserId;

    @Id
    @Column(name = "high_user_id")
    private Long highUserId;

    @Getter
    @Column(updatable = false)
    private LocalDate createdAt;

    @Getter
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "ownerId",
                    column = @Column(name = "low_user_id", insertable = false, updatable = false)),
            @AttributeOverride(name = "friendAlias", column = @Column(name = "low_friend_alias")),
            @AttributeOverride(name = "interestScore",
                    column = @Column(name = "low_interest_score", updatable = false)),
            @AttributeOverride(name = "lastInteractedAt",
                    column = @Column(name = "low_last_interacted_at", updatable = false)),
            @AttributeOverride(name = "isMuted", column = @Column(name = "low_is_muted")),
            @AttributeOverride(name = "isRoutable", column = @Column(name = "low_is_routable"))
    })
    private FriendRecognition lower;

    @Getter
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "ownerId",
                    column = @Column(name = "high_user_id", insertable = false, updatable = false)),
            @AttributeOverride(name = "friendAlias", column = @Column(name = "high_friend_alias")),
            @AttributeOverride(name = "interestScore",
                    column = @Column(name = "high_interest_score", updatable = false)),
            @AttributeOverride(name = "lastInteractedAt",
                    column = @Column(name = "high_last_interacted_at", updatable = false)),
            @AttributeOverride(name = "isMuted", column = @Column(name = "high_is_muted")),
            @AttributeOverride(name = "isRoutable", column = @Column(name = "high_is_routable"))
    })
    private FriendRecognition higher;

    Friendship(Long userA, Long userB) {
        validateUserIds(userA, userB);
        this.lower = new FriendRecognition(Math.min(userA, userB));
        this.higher = new FriendRecognition(Math.max(userA, userB));
        this.lowUserId = lower.getOwnerId();
        this.highUserId = higher.getOwnerId();
        this.createdAt = LocalDate.now();
    }

    private Friendship(LocalDate createdAt, FriendRecognition first, FriendRecognition second) {
        this.createdAt = createdAt;
        this.lower = first.getOwnerId() < second.getOwnerId() ? first : second;
        this.higher = first.getOwnerId() < second.getOwnerId() ? second : first;
        this.lowUserId = lower.getOwnerId();
        this.highUserId = higher.getOwnerId();
    }

    public static Friendship restore(LocalDate createdAt, FriendRecognition first, FriendRecognition second) {
        if (first == null || second == null) {
            throw new FriendshipInvalidException("missing recognition");
        }
        validateUserIds(first.getOwnerId(), second.getOwnerId());
        return new Friendship(createdAt, first, second);
    }

    private static void validateUserIds(Long first, Long second) {
        if (first == null || second == null || first.equals(second)) {
            throw new FriendshipInvalidException(first + "_" + second);
        }
    }

    private FriendRecognition getSelfRecognition(Long userId) {
        if (lower.getOwnerId().equals(userId)) return lower;
        if (higher.getOwnerId().equals(userId)) return higher;
        throw new FriendshipAuthorizationException(userId);
    }

    private FriendRecognition getFriendRecognition(Long myId) {
        return getSelfRecognition(myId) == lower ? higher : lower;
    }

    public Set<Long> getUserIds() {
        return Set.of(lower.getOwnerId(), higher.getOwnerId());
    }

    public String getFriendAlias(Long myId) {
        return getSelfRecognition(myId).getFriendAlias();
    }

    public double getIntimacy() {
        return Math.sqrt(lower.getNormalizedScore() * higher.getNormalizedScore());
    }

    public double getMyInterestScore(Long myId) {
        return getSelfRecognition(myId).getInterestScore();
    }

    public double getMyNormalizedInterestScore(Long myId) {
        return getSelfRecognition(myId).getNormalizedScore();
    }

    public Long getFriendId(Long myId) {
        return getFriendRecognition(myId).getOwnerId();
    }

    public void adjustInterestScore(Long userId, double scoreDelta) {
        getSelfRecognition(userId).adjustInterestScore(scoreDelta);
    }

    public void adjustMutualInterestScore(double scoreDelta) {
        lower.adjustInterestScore(scoreDelta);
        higher.adjustInterestScore(scoreDelta);
    }

    public void decay(double rate, double minThreshold, LocalDateTime thresholdTime) {
        lower.decay(rate, minThreshold, thresholdTime);
        higher.decay(rate, minThreshold, thresholdTime);
    }

    public boolean isMuted(Long myId) {
        return getSelfRecognition(myId).isMuted();
    }

    public boolean isRoutable(Long myId) {
        return getSelfRecognition(myId).isRoutable();
    }

    public void updateSettings(Long ownerId, String alias, Boolean isMuted, Boolean isRoutable) {
        FriendRecognition rec = getSelfRecognition(ownerId);
        if (alias != null)      rec.updateFriendAlias(alias.isEmpty() ? null : alias);
        if (isMuted != null)    rec.updateMuteStatus(isMuted);
        if (isRoutable != null) rec.updateRoutableStatus(isRoutable);
    }
}
