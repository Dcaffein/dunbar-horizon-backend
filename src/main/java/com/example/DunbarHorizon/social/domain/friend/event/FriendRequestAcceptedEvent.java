package com.example.DunbarHorizon.social.domain.friend.event;

public record FriendRequestAcceptedEvent(
        String requestId,
        Long requesterId,
        Long receiverId,
        String receiverNickname) {
}
