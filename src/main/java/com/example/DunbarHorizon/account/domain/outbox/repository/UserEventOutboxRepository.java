package com.example.DunbarHorizon.account.domain.outbox.repository;

import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserEventOutboxRepository {
    UserEventOutbox save(UserEventOutbox outbox);
    Optional<UserEventOutbox> findById(String id);
    List<UserEventOutbox> findPendingOlderThan(LocalDateTime threshold);
}
