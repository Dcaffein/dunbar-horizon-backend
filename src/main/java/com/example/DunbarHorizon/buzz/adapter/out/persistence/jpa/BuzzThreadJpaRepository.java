package com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuzzThreadJpaRepository extends JpaRepository<BuzzThread, Long> {

    List<BuzzThread> findAllByBuzzIdOrderByCreatedAtAscIdAsc(String buzzId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT t FROM BuzzThread t WHERE t.id = :id")
    Optional<BuzzThread> findByIdForShare(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BuzzThread t WHERE t.id = :id")
    void deleteById(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BuzzThread t WHERE t.buzzId = :buzzId")
    int deleteAllByBuzzId(@Param("buzzId") String buzzId);
}
