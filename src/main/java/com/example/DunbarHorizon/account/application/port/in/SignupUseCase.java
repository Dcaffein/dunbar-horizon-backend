package com.example.DunbarHorizon.account.application.port.in;

import com.example.DunbarHorizon.account.domain.AuthProvider;
import com.example.DunbarHorizon.account.domain.User;

public interface SignupUseCase {
    void signup(String email, String password, String nickname);
    User registerOAuthUser(String email, String nickname, AuthProvider provider, String providerId);
}