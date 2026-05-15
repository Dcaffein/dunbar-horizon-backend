package com.example.DunbarHorizon.account.domain.repository;

import java.util.Optional;

public interface EmailVerificationTokenRepository {
    void save(Long userId, String token);
    Optional<Long> findUserIdByToken(String token);
    void deleteByUserId(Long userId);
}
