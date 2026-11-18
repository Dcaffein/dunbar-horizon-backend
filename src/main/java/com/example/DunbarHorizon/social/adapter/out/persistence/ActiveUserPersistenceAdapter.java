package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ActiveUserJpaRepository;
import com.example.DunbarHorizon.social.application.port.out.ActiveUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ActiveUserPersistenceAdapter implements ActiveUserRepository {

    private final ActiveUserJpaRepository activeUserJpaRepository;

    @Override
    public boolean existsById(Long userId) {
        return activeUserJpaRepository.existsById(userId);
    }
}
