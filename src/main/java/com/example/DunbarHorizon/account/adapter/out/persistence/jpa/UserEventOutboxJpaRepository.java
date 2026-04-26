package com.example.DunbarHorizon.account.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserEventOutboxJpaRepository extends JpaRepository<UserEventOutbox, String> {

    @Query("SELECT o FROM UserEventOutbox o WHERE o.status = :status AND o.createdAt < :threshold")
    List<UserEventOutbox> findByStatusAndCreatedAtBefore(
            @Param("status") UserOutboxStatus status,
            @Param("threshold") LocalDateTime threshold
    );
}
