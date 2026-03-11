package com.example.DunbarHorizon.account.application.port.in;

import com.example.DunbarHorizon.account.domain.model.AuthProvider;
import com.example.DunbarHorizon.account.domain.model.User;

public interface SignupUseCase {
    void signup(String email, String password, String nickname);
    User registerOAuthUser(String email, String nickname, AuthProvider provider, String providerId);
}