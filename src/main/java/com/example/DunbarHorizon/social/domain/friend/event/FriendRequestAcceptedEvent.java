package com.example.DunbarHorizon.social.domain.friend.event;

public record FriendRequestAcceptedEvent(
        Long requesterId,
        Long receiverId,
        String receiverNickname
) {}
