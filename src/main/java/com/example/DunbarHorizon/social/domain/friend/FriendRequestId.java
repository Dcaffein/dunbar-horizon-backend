package com.example.DunbarHorizon.social.domain.friend;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequestId implements Serializable {

    private Long requesterId;

    private Long receiverId;

    public FriendRequestId(Long requesterId, Long receiverId) {
        this.requesterId = requesterId;
        this.receiverId = receiverId;
    }
}
