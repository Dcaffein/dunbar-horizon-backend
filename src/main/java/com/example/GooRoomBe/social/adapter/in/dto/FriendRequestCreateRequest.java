package com.example.GooRoomBe.social.adapter.in.dto;

import jakarta.validation.constraints.NotNull;

public record FriendRequestCreateRequest(
        @NotNull(message = "친구 요청을 보낼 대상(receiverId)은 필수입니다.")
        Long receiverId
) {}