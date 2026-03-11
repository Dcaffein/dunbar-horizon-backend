package com.example.DunbarHorizon.account.domain.repository;

import com.example.DunbarHorizon.account.domain.model.Auth;
import com.example.DunbarHorizon.account.domain.model.AuthProvider;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthRepository {
    Optional<Auth> findByUserIdAndProvider(Long userId, AuthProvider provider);
    Auth save(Auth auth);
    boolean existsByUserIdAndProviderAndProviderId(Long userId, AuthProvider provider, String providerId);

    void deleteAllByUserId(Long userId);

    void deleteUnverifiedByUserId(Long userId);

    int deleteOldUnverifiedAuths(LocalDateTime threshold);
}