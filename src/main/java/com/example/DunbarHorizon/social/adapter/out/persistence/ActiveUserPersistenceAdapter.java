package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ActiveUserJpaRepository;
import com.example.DunbarHorizon.social.application.port.out.ActiveUserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ActiveUserPersistenceAdapter implements ActiveUserLookup {

    private final ActiveUserJpaRepository activeUserJpaRepository;

    @Override
    public boolean isActive(Long userId) {
        return activeUserJpaRepository.existsById(userId);
    }
}
