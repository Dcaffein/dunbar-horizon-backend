package com.example.GooRoomBe.social.friend.domain;

import com.example.GooRoomBe.social.friend.exception.FriendshipAuthorizationException;
import com.example.GooRoomBe.social.friend.exception.FriendshipNotFoundException;
import com.example.GooRoomBe.social.socialUser.SocialUser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.GooRoomBe.social.common.SocialSchemaConstants.MEMBER_OF;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Node("FriendShip")
public class Friendship {
    @Getter
    @Id
    private String id;

    @Getter
    private LocalDate createdAt;

    @Getter
    private double intimacy;

    @Relationship(type = MEMBER_OF, direction = Relationship.Direction.INCOMING)
    private List<FriendRecognition> recognitions = new ArrayList<>();

    public Friendship(SocialUser userA, SocialUser userB) {
        this.id = generateCompositeId(userA.getId(), userB.getId());
        this.createdAt = LocalDate.now();

        this.recognitions.add(new FriendRecognition(userA));
        this.recognitions.add(new FriendRecognition(userB));
    }

    @PersistenceCreator
    public Friendship(String id, LocalDate createdAt, List<FriendRecognition> recognitions) {
        this.id = id;
        this.createdAt = createdAt;
        this.recognitions = recognitions;
    }

    private FriendRecognition getSelfRecognition(String userId) {
        return this.recognitions.stream()
                .filter(friendRecognition -> friendRecognition.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new FriendshipNotFoundException(this.id, userId));
    }

    private FriendRecognition getFriendRecognition(String myId) {
        boolean isMember = this.recognitions.stream()
                .anyMatch(friendRecognition -> friendRecognition.getUser().getId().equals(myId));
        if (!isMember) {
            throw new FriendshipAuthorizationException(myId);
        }

        //
        return this.recognitions.stream()
                .filter(friendRecognition -> !friendRecognition.getUser().getId().equals(myId))
                .findFirst()
                .orElseThrow(() -> new FriendshipAuthorizationException(myId));
    }

    private static String generateCompositeId(String id1, String id2) {
        return Stream.of(id1, id2)
                .sorted()
                .collect(Collectors.joining("_"));
    }

    public Set<SocialUser> getUsers() {
        return this.recognitions.stream()
                .map(FriendRecognition::getUser)
                .collect(Collectors.toSet());
    }

    public String getFriendAlias(String myId) {
        return getSelfRecognition(myId).getFriendAlias();
    }

    public void updateFriendAlias(String myId, String alias) {
        getSelfRecognition(myId).updateFriendAlias(alias);
    }

    public void updateOnIntroduce(String myId, boolean onIntroduce) {
        getSelfRecognition(myId).updateOnIntroduce(onIntroduce);
    }

    public SocialUser getFriend(String myId) {
        return getFriendRecognition(myId).getUser();
    }

    public void checkDeletable(String myId) {
        boolean isParticipant = this.recognitions.stream()
                .anyMatch(r -> r.getUser().getId().equals(myId));

        if (!isParticipant) {
            throw new FriendshipAuthorizationException(myId);
        }
    }

    public void adjustInterestScore(String userId, double scoreDelta) {
        getSelfRecognition(userId).adjustInterestScore(scoreDelta);
        recalculateIntimacy();
    }

    private void recalculateIntimacy() {
        if (this.recognitions.size() < 2) {
            this.intimacy = 0.0;
            return;
        }

        double affinityA = Math.max(0, this.recognitions.get(0).getInterestScore());
        double affinityB = Math.max(0, this.recognitions.get(1).getInterestScore());

        this.intimacy = Math.sqrt(affinityA * affinityB);
    }
}