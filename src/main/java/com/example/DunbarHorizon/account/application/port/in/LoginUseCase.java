package com.example.DunbarHorizon.account.application.port.in;

import com.example.DunbarHorizon.account.application.dto.AuthTokenResult;
import com.example.DunbarHorizon.account.domain.model.User;

public interface LoginUseCase {
    AuthTokenResult login(String email, String password);
    AuthTokenResult issueTokens(User user);
    AuthTokenResult reissue(String refreshToken);
    void logout(String refreshToken);
}
