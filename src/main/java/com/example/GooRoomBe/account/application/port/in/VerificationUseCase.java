package com.example.GooRoomBe.account.application.port.in;

public interface VerificationUseCase {
    void sendVerificationEmail(String email, String redirectPage);
    void verifyEmail(String token);
}
