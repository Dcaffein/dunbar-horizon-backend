package com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BuzzThreadJpaRepository extends JpaRepository<BuzzThread, Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT t FROM BuzzThread t WHERE t.id = :id")
    Optional<BuzzThread> findByIdForShare(@Param("id") Long id);
}
