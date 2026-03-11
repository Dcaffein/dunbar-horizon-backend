package com.example.DunbarHorizon.account.application.port.out;

public interface PasswordHasher {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}