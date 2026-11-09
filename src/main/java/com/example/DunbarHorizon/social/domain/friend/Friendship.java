package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipAuthorizationException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipInvalidException;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Friendship {

    public static final int ALIAS_MAX_LENGTH = 20;
    public static final String ALIAS_LENGTH_MESSAGE = "별명은 20자 이하로 입력해주세요.";

    @Getter
    private final String id;

    @Getter
    private final LocalDate createdAt;

    @Getter
    private double intimacy;

    private final List<FriendRecognition> recognitions;

    Friendship(UserReference userA, UserReference userB) {
        FriendRecognition recognitionA = new FriendRecognition(userA);
        FriendRecognition recognitionB = new FriendRecognition(userB);

        this.recognitions = new ArrayList<>(List.of(recognitionA, recognitionB));
        this.id = generateCompositeId(userA.getId(), userB.getId());
        this.createdAt = LocalDate.now();

        this.intimacy = Math.sqrt(recognitionA.getNormalizedScore() * recognitionB.getNormalizedScore());
    }

    private Friendship(String id, LocalDate createdAt, double intimacy, List<FriendRecognition> recognitions) {
        this.id = id;
        this.createdAt = createdAt;
        this.intimacy = intimacy;
        this.recognitions = new ArrayList<>(recognitions);
    }

    public static Friendship restore(String id, LocalDate createdAt, double intimacy,
                                     List<FriendRecognition> recognitions) {
        if (recognitions == null || recognitions.size() != 2) {
            throw new FriendshipInvalidException("Friendship은 반드시 2명의 구성원을 가져야 합니다. ID: " + id);
        }
        return new Friendship(id, createdAt, intimacy, recognitions);
    }

    private FriendRecognition getSelfRecognition(Long userId) {
        return this.recognitions.stream()
                .filter(friendRecognition -> friendRecognition.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new FriendshipAuthorizationException(userId));
    }

    private FriendRecognition getFriendRecognition(Long myId) {
        boolean isMember = this.recognitions.stream()
                .anyMatch(friendRecognition -> friendRecognition.getUser().getId().equals(myId));
        if (!isMember) {
            throw new FriendshipAuthorizationException(myId);
        }

        return this.recognitions.stream()
                .filter(friendRecognition -> !friendRecognition.getUser().getId().equals(myId))
                .findFirst()
                .orElseThrow(() -> new FriendshipAuthorizationException(myId));
    }

    public static String generateCompositeId(Long id1, Long id2) {
        long min = Math.min(id1, id2);
        long max = Math.max(id1, id2);
        return min + "_" + max;
    }

    public Set<UserReference> getUsers() {
        return this.recognitions.stream()
                .map(FriendRecognition::getUser)
                .collect(Collectors.toSet());
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

    public UserReference getFriend(Long myId) {
        return getFriendRecognition(myId).getUser();
    }

    public void updateFriendAlias(Long myId, String alias) {
        getSelfRecognition(myId).updateFriendAlias(alias);
    }

    public void adjustInterestScore(Long userId, double scoreDelta) {
        getSelfRecognition(userId).adjustInterestScore(scoreDelta);
        recalculateIntimacy();
    }

    public void adjustMutualInterestScore(double scoreDelta) {
        recognitions.forEach(r -> r.adjustInterestScore(scoreDelta));
        recalculateIntimacy();
    }

    public void decay(double rate, double minThreshold, LocalDateTime thresholdTime) {
        boolean isTarget = recognitions.stream().anyMatch(r -> r.isDecayTarget(thresholdTime));
        if (!isTarget) {
            return;
        }
        recognitions.forEach(r -> r.decay(rate, minThreshold, thresholdTime));
        recalculateIntimacy();
    }

    public void updateMuteStatus(Long myId, boolean isMuted) {
        getSelfRecognition(myId).updateMuteStatus(isMuted);
    }

    public void updateRoutableStatus(Long myId, boolean isRoutable) {
        getSelfRecognition(myId).updateRoutableStatus(isRoutable);
    }

    private void recalculateIntimacy() {
        double a = Math.max(0, this.recognitions.get(0).getNormalizedScore());
        double b = Math.max(0, this.recognitions.get(1).getNormalizedScore());
        this.intimacy = Math.sqrt(a * b);
    }

    public boolean isMuted(Long myId) {
        return getSelfRecognition(myId).isMuted();
    }

    public boolean isRoutable(Long myId) {
        return getSelfRecognition(myId).isRoutable();
    }

    public List<FriendRecognition> getRecognitions() {
        return List.copyOf(recognitions);
    }

    public void updateUserFields(Long userId, String alias, Boolean isMuted, Boolean isRoutable) {
        FriendRecognition rec = getSelfRecognition(userId);
        if (alias != null)      rec.updateFriendAlias(alias.isEmpty() ? null : alias);
        if (isMuted != null)    rec.updateMuteStatus(isMuted);
        if (isRoutable != null) rec.updateRoutableStatus(isRoutable);
    }
}