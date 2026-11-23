package com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BuzzJpaRepository extends JpaRepository<Buzz, Long> {

    Optional<Buzz> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT b FROM Buzz b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Buzz> findByIdAndDeletedAtIsNullForShare(@Param("id") Long id);
}
