package com.example.GooRoomBe.social.domain.friend.event;

public record FriendRequestAcceptedEvent(
        String requestId,
        Long requesterId,
        Long receiverId,
        String receiverNickname) {
}
