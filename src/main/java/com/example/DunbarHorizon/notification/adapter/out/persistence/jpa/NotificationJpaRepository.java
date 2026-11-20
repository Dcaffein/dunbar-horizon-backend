package com.example.DunbarHorizon.notification.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {
    Slice<Notification> findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long receiverId, LocalDateTime cutoff, Pageable pageable);

    long countByReceiverIdAndIsReadFalseAndCreatedAtAfter(Long receiverId, LocalDateTime cutoff);

    Optional<Notification> findByIdAndCreatedAtAfter(Long id, LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isSent = true WHERE n.id IN :ids")
    int updateIsSentTrueByIdIn(@Param("ids") Collection<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.createdAt > :cutoff")
    int updateIsReadTrueByIdAndCreatedAtAfter(@Param("id") Long id, @Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.createdAt <= :cutoff")
    int deleteAllByCreatedAtAtOrBefore(@Param("cutoff") LocalDateTime cutoff);
}
