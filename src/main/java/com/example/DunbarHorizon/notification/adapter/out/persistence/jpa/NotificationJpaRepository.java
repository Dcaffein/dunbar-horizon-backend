package com.example.DunbarHorizon.notification.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {
    Slice<Notification> findAllByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);
    long countByReceiverIdAndIsReadFalse(Long receiverId);
}
