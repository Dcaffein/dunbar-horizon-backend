package com.example.DunbarHorizon.social.application.dto;

import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;

import java.time.LocalDateTime;

public record FriendRequestView(
        Long requesterId,
        String requesterNickname,
        Long receiverId,
        String receiverNickname,
        FriendRequestStatus status,
        LocalDateTime createdAt
) {}
