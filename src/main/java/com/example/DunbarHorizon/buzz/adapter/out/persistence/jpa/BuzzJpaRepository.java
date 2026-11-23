package com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BuzzJpaRepository extends JpaRepository<Buzz, Long> {

    Optional<Buzz> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT b FROM Buzz b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Buzz> findByIdAndDeletedAtIsNullForShare(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Buzz b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Buzz> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);

    @Query("SELECT b.id FROM Buzz b WHERE b.createdAt <= :createdAt")
    List<Long> findIdsByCreatedAtAtOrBefore(@Param("createdAt") LocalDateTime createdAt);

    @Query("SELECT b.id FROM Buzz b WHERE b.deletedAt IS NOT NULL")
    List<Long> findIdsByDeletedAtIsNotNull();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Buzz b WHERE b.id IN :ids")
    int deleteAllByIdIn(@Param("ids") Collection<Long> ids);
}
