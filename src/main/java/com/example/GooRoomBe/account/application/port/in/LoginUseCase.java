package com.example.GooRoomBe.account.application.port.in;

import com.example.GooRoomBe.account.application.port.in.dto.AuthTokenResult;
import com.example.GooRoomBe.account.domain.model.User;

public interface LoginUseCase {
    AuthTokenResult login(String email, String password);
    AuthTokenResult issueTokens(User user);
    AuthTokenResult reissue(String refreshToken);
    void logout(String refreshToken);
}
