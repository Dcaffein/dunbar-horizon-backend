package com.example.GooRoomBe.account.application.port.out;

public interface EmailPort {
    void sendVerificationEmail(String email, String token, String redirectPage);
}