package com.example.GooRoomBe.social.application.port.in;

import com.example.GooRoomBe.social.application.port.in.dto.FriendRequestResponse;

import java.util.List;

public interface FriendRequestQueryUseCase {
    List<FriendRequestResponse> getReceivedRequests(Long userId);
}
