package com.example.DunbarHorizon.notification.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {
    Slice<Notification> findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long receiverId, LocalDateTime cutoff, Pageable pageable);

    long countByReceiverIdAndIsReadFalseAndCreatedAtAfter(Long receiverId, LocalDateTime cutoff);

    Optional<Notification> findByIdAndCreatedAtAfter(Long id, LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.createdAt <= :cutoff")
    int deleteAllByCreatedAtAtOrBefore(@Param("cutoff") LocalDateTime cutoff);
}
