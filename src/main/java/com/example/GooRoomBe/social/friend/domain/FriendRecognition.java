package com.example.GooRoomBe.social.friend.domain;

import com.example.GooRoomBe.global.userReference.SocialUser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RelationshipProperties
@Getter
public class FriendRecognition {
    private static final double DEFAULT_DECAY_RATE = 0.95;

    @Id @GeneratedValue
    private Long id;

    @TargetNode
    private SocialUser user;

    private boolean onIntroduce;

    private String friendAlias;

    private Double interestScore = 0.0;

    private LocalDateTime lastInteractedAt;

    public FriendRecognition(SocialUser user) {
        this.user = user;
        this.friendAlias = null;
        this.onIntroduce = false;
        this.lastInteractedAt = LocalDateTime.now();
    }


    public void updateFriendAlias(String newAlias) {
        this.friendAlias = newAlias;
    }

    public void updateOnIntroduce(boolean onIntroduce) {
        this.onIntroduce = onIntroduce;
    }

    public String getFriendAlias() {
        return friendAlias == null ? "" : friendAlias;
    }

    public Double getInterestScore() {
        return this.interestScore == null ? 0.0 : this.interestScore;
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
}
