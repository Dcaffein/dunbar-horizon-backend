package com.example.DunbarHorizon.notification.adapter.out.persistence.mongo;

import com.example.DunbarHorizon.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationMongoRepository extends MongoRepository<Notification, String> {
    Slice<Notification> findAllByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);
    long countByReceiverIdAndIsReadFalse(Long receiverId);
    List<Notification> findByIsSentFalse();
    void deleteByCreatedAtBefore(LocalDateTime threshold);
}
