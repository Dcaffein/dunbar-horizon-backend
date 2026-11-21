package com.example.DunbarHorizon.notification.domain.repository;

import com.example.DunbarHorizon.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Slice<Notification> findAllRetainedByReceiverId(Long receiverId, LocalDateTime asOf, Pageable pageable);
    long countRetainedUnreadByReceiverId(Long receiverId, LocalDateTime asOf);
    Optional<Notification> findById(Long id);
    Optional<Notification> findRetainedById(Long id, LocalDateTime asOf);
    List<Notification> saveAll(List<Notification> notifications);
    void deleteById(Long id);
    int markAsRead(Long id, LocalDateTime asOf);
    int purgeExpired(LocalDateTime asOf);
}
