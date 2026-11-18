package com.example.DunbarHorizon.social.domain.friend;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendshipId implements Serializable {

    private Long lowUserId;

    private Long highUserId;

    private FriendshipId(Long lowUserId, Long highUserId) {
        this.lowUserId = lowUserId;
        this.highUserId = highUserId;
    }

    public static FriendshipId of(long firstUserId, long secondUserId) {
        return new FriendshipId(Math.min(firstUserId, secondUserId), Math.max(firstUserId, secondUserId));
    }
}
