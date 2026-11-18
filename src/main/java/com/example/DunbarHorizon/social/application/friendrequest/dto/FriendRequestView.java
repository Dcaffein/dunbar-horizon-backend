package com.example.DunbarHorizon.social.application.friendrequest.dto;

import com.example.DunbarHorizon.social.domain.friendship.FriendRequestStatus;

import java.time.LocalDateTime;

public record FriendRequestView(
        Long requesterId,
        String requesterNickname,
        Long receiverId,
        String receiverNickname,
        FriendRequestStatus status,
        LocalDateTime createdAt
) {}
