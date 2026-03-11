package com.example.DunbarHorizon.account.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.AuthJpaRepository;
import com.example.DunbarHorizon.account.domain.model.Auth;
import com.example.DunbarHorizon.account.domain.model.AuthProvider;
import com.example.DunbarHorizon.account.domain.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthRepositoryAdapter implements AuthRepository {

    private final AuthJpaRepository authJpaRepository;

    @Override
    public Optional<Auth> findByUserIdAndProvider(Long userId, AuthProvider provider) {
        return authJpaRepository.findByUserIdAndProvider(userId, provider);
    }

    @Override
    public Auth save(Auth auth) {
        return authJpaRepository.save(auth);
    }

    @Override
    public boolean existsByUserIdAndProviderAndProviderId(Long userId, AuthProvider provider, String providerId) {
        return authJpaRepository.existsByUserIdAndProviderAndProviderId(userId, provider, providerId);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        authJpaRepository.deleteAllByUserId(userId);
    }

    @Override
    public void deleteUnverifiedByUserId(Long userId) {
        authJpaRepository.deleteUnverifiedByUserId(userId);
    }

    @Override
    public int deleteOldUnverifiedAuths(LocalDateTime threshold) {
        return authJpaRepository.deleteOldUnverifiedAuths(threshold);
    }
}
