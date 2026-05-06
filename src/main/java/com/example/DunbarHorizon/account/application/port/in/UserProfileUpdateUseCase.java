package com.example.DunbarHorizon.account.application.port.in;

import com.example.DunbarHorizon.account.application.model.UploadFile;

public interface UserProfileUpdateUseCase {
    void updateProfile(Long userId, String nickname, UploadFile profileImage);
}
