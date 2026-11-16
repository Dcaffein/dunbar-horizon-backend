package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipAuthorizationException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipInvalidException;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public class Friendship {

    public static final int ALIAS_MAX_LENGTH = 20;
    public static final String ALIAS_LENGTH_MESSAGE = "별명은 20자 이하로 입력해주세요.";

    @Getter
    private final LocalDate createdAt;

    @Getter
    private final FriendRecognition lower;

    @Getter
    private final FriendRecognition higher;

    Friendship(Long userA, Long userB) {
        validateUserIds(userA, userB);
        this.lower = new FriendRecognition(Math.min(userA, userB));
        this.higher = new FriendRecognition(Math.max(userA, userB));
        this.createdAt = LocalDate.now();
    }

    private Friendship(LocalDate createdAt, FriendRecognition first, FriendRecognition second) {
        this.createdAt = createdAt;
        this.lower = first.getOwnerId() < second.getOwnerId() ? first : second;
        this.higher = first.getOwnerId() < second.getOwnerId() ? second : first;
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

    public void updateUserFields(Long userId, String alias, Boolean isMuted, Boolean isRoutable) {
        FriendRecognition rec = getSelfRecognition(userId);
        if (alias != null)      rec.updateFriendAlias(alias.isEmpty() ? null : alias);
        if (isMuted != null)    rec.updateMuteStatus(isMuted);
        if (isRoutable != null) rec.updateRoutableStatus(isRoutable);
    }
}
