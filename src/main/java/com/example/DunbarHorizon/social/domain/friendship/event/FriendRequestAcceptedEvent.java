package com.example.DunbarHorizon.social.domain.friendship.event;

public record FriendRequestAcceptedEvent(
        Long requesterId,
        Long receiverId
) {}
