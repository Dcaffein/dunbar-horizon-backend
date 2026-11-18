package com.example.DunbarHorizon.social.domain.friend;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendshipId implements Serializable {

    private Long lowUserId;

    private Long highUserId;

    public FriendshipId(Long lowUserId, Long highUserId) {
        this.lowUserId = lowUserId;
        this.highUserId = highUserId;
    }
}
