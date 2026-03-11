package com.example.DunbarHorizon.account.application.port.in;

public interface VerificationUseCase {
    void sendVerificationEmail(String email, String redirectPage);
    void verifyEmail(String token);
}
