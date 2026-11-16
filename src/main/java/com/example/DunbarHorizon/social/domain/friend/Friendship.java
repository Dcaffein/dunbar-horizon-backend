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
    private double intimacy;

    @Getter
    private final FriendRecognition lower;

    @Getter
    private final FriendRecognition higher;

    Friendship(Long userA, Long userB) {
        validateUserIds(userA, userB);
        this.lower = new FriendRecognition(Math.min(userA, userB));
        this.higher = new FriendRecognition(Math.max(userA, userB));
        this.createdAt = LocalDate.now();
        recalculateIntimacy();
    }

    private Friendship(LocalDate createdAt, double intimacy, FriendRecognition first, FriendRecognition second) {
        this.createdAt = createdAt;
        this.intimacy = intimacy;
        this.lower = first.getUserId() < second.getUserId() ? first : second;
        this.higher = first.getUserId() < second.getUserId() ? second : first;
    }

    public static Friendship restore(LocalDate createdAt, double intimacy,
                                     FriendRecognition first, FriendRecognition second) {
        if (first == null || second == null) {
            throw new FriendshipInvalidException("missing recognition");
        }
        validateUserIds(first.getUserId(), second.getUserId());
        return new Friendship(createdAt, intimacy, first, second);
    }

    private static void validateUserIds(Long first, Long second) {
        if (first == null || second == null || first.equals(second)) {
            throw new FriendshipInvalidException(first + "_" + second);
        }
    }

    public String getId() {
        return generateCompositeId(lower.getUserId(), higher.getUserId());
    }

    private FriendRecognition getSelfRecognition(Long userId) {
        if (lower.getUserId().equals(userId)) return lower;
        if (higher.getUserId().equals(userId)) return higher;
        throw new FriendshipAuthorizationException(userId);
    }

    private FriendRecognition getFriendRecognition(Long myId) {
        return getSelfRecognition(myId) == lower ? higher : lower;
    }

    public static String generateCompositeId(Long id1, Long id2) {
        long min = Math.min(id1, id2);
        long max = Math.max(id1, id2);
        return min + "_" + max;
    }

    public Set<Long> getUserIds() {
        return Set.of(lower.getUserId(), higher.getUserId());
    }

    public String getFriendAlias(Long myId) {
        return getSelfRecognition(myId).getFriendAlias();
    }

    public Double getMyInterestScore(Long myId) {
        return getSelfRecognition(myId).getInterestScore();
    }

    public double getMyNormalizedInterestScore(Long myId) {
        return getSelfRecognition(myId).getNormalizedScore();
    }

    public Long getFriendId(Long myId) {
        return getFriendRecognition(myId).getUserId();
    }

    public void adjustInterestScore(Long userId, double scoreDelta) {
        getSelfRecognition(userId).adjustInterestScore(scoreDelta);
        recalculateIntimacy();
    }

    public void adjustMutualInterestScore(double scoreDelta) {
        lower.adjustInterestScore(scoreDelta);
        higher.adjustInterestScore(scoreDelta);
        recalculateIntimacy();
    }

    public void decay(double rate, double minThreshold, LocalDateTime thresholdTime) {
        boolean isTarget = lower.isDecayTarget(thresholdTime) || higher.isDecayTarget(thresholdTime);
        if (!isTarget) {
            return;
        }
        lower.decay(rate, minThreshold, thresholdTime);
        higher.decay(rate, minThreshold, thresholdTime);
        recalculateIntimacy();
    }

    private void recalculateIntimacy() {
        double a = Math.max(0, lower.getNormalizedScore());
        double b = Math.max(0, higher.getNormalizedScore());
        this.intimacy = Math.sqrt(a * b);
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
