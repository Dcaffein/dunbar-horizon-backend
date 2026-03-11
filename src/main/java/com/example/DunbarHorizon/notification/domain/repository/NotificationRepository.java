package com.example.DunbarHorizon.notification.domain.repository;

import com.example.DunbarHorizon.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository{
    Slice<Notification> findAllByReceiverId(Long receiverId, Pageable pageable);
    long countByReceiverIdAndIsReadFalse(Long receiverId);
    List<Notification> findByIsSentFalse();
    void deleteByCreatedAtBefore(LocalDateTime threshold);
    Notification save(Notification notification);
    Optional<Notification> findById(String id);
    List<Notification> saveAll(List<Notification> notifications);
}