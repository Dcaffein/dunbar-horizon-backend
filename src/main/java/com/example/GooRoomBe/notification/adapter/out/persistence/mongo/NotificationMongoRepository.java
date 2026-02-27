package com.example.GooRoomBe.notification.adapter.out.persistence.mongo;

import com.example.GooRoomBe.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationMongoRepository extends MongoRepository<Notification, String> {
    Slice<Notification> findAllByReceiverId(Long receiverId, Pageable pageable);
    long countByReceiverIdAndIsReadFalse(Long receiverId);
    List<Notification> findByIsSentFalse();
    void deleteByCreatedAtBefore(LocalDateTime threshold);
}
