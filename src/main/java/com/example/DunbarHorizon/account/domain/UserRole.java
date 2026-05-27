package com.example.DunbarHorizon.account.domain;

import lombok.Getter;

@Getter
public enum UserRole {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String key;

    UserRole(String key) {
        this.key = key;
    }
}