package com.example.GooRoomBe.account.application.port.in;

import com.example.GooRoomBe.account.domain.model.AuthProvider;
import com.example.GooRoomBe.account.domain.model.User;

public interface SignupUseCase {
    void signup(String email, String password, String nickname);
    User registerOAuthUser(String email, String nickname, AuthProvider provider, String providerId);
}