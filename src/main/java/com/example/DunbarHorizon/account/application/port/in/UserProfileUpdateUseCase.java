package com.example.DunbarHorizon.account.application.port.in;

public interface UserProfileUpdateUseCase {
    void updateProfile(Long userId, String nickname, String profileImageUrl);
}
