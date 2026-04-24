package com.example.DunbarHorizon.social.domain.friend;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipAuthorizationException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendshipInvalidException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.DunbarHorizon.social.domain.friend.constant.FriendConstants.HAS_FRIENDSHIP;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Node("Friendship")
public class Friendship {
    @Getter
    @Id
    private String id;

    @Version
    private Long version;

    @Getter
    private LocalDate createdAt;

    @Getter
    private double intimacy;

    @Relationship(type = HAS_FRIENDSHIP, direction = Relationship.Direction.INCOMING)
    private List<FriendRecognition> recognitions = new ArrayList<>();

    Friendship(UserReference userA, UserReference userB) {
        FriendRecognition recognitionA = new FriendRecognition(userA);
        FriendRecognition recognitionB = new FriendRecognition(userB);

        this.recognitions.add(recognitionA);
        this.recognitions.add(recognitionB);
        this.id = generateCompositeId(userA.getId(), userB.getId());
        this.createdAt = LocalDate.now();

        this.intimacy = calculateIntimacyPolicy(
                recognitionA.getNormalizedScore(),
                recognitionB.getNormalizedScore()
        );
    }

    @PersistenceCreator
    public Friendship(String id, LocalDate createdAt, List<FriendRecognition> recognitions) {
        if (recognitions == null || recognitions.size() != 2) {
            throw new FriendshipInvalidException("Friendship은 반드시 2명의 구성원을 가져야 합니다. ID: " + id);
        }
        this.id = id;
        this.createdAt = createdAt;
        this.recognitions.addAll(recognitions);
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

    public void updateMuteStatus(Long myId, boolean isMuted) {
        getSelfRecognition(myId).updateMuteStatus(isMuted);
    }

    public void updateRoutableStatus(Long myId, boolean isRoutable) {
        getSelfRecognition(myId).updateRoutableStatus(isRoutable);
    }

    private void recalculateIntimacy() {
        if (this.recognitions.size() < 2) {
            this.intimacy = 0.0;
            return;
        }

        double affinityA = Math.max(0, this.recognitions.get(0).getNormalizedScore());
        double affinityB = Math.max(0, this.recognitions.get(1).getNormalizedScore());

        this.intimacy = calculateIntimacyPolicy(affinityA, affinityB);
    }

    private static double calculateIntimacyPolicy(double scoreA, double scoreB) {
        return Math.sqrt(scoreA * scoreB);
    }

    public boolean isMuted(Long myId) {
        return getSelfRecognition(myId).isMuted();
    }

    public boolean isRoutable(Long myId) {
        return getSelfRecognition(myId).isRoutable();
    }
}