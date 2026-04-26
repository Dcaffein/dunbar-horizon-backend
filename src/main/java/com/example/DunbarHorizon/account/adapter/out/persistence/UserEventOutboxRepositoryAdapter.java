package com.example.DunbarHorizon.account.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserEventOutboxJpaRepository;
import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxStatus;
import com.example.DunbarHorizon.account.domain.outbox.repository.UserEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserEventOutboxRepositoryAdapter implements UserEventOutboxRepository {

    private final UserEventOutboxJpaRepository jpaRepository;

    @Override
    public UserEventOutbox save(UserEventOutbox outbox) {
        return jpaRepository.save(outbox);
    }

    @Override
    public Optional<UserEventOutbox> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<UserEventOutbox> findPendingOlderThan(LocalDateTime threshold) {
        return jpaRepository.findByStatusAndCreatedAtBefore(UserOutboxStatus.PENDING, threshold);
    }
}
