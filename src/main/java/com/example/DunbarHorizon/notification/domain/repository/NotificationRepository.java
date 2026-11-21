package com.example.DunbarHorizon.notification.domain.repository;

import com.example.DunbarHorizon.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Slice<Notification> findAllRetainedByReceiverId(Long receiverId, LocalDateTime now, Pageable pageable);
    long countRetainedUnreadByReceiverId(Long receiverId, LocalDateTime now);
    Optional<Notification> findById(Long id);
    Optional<Notification> findRetainedById(Long id, LocalDateTime now);
    Notification save(Notification notification);
    List<Notification> saveAll(List<Notification> notifications);
    void deleteById(Long id);
    int purgeExpired(LocalDateTime now);
}
