package com.example.GooRoomBe.social.application.port.in;

public interface FriendRequestReceiverActionUseCase {
    void acceptRequest(String requestId, Long receiverId);
    void hideRequest(String requestId, Long receiverId);
    void undoHideRequest(String requestId, Long receiverId);
}
