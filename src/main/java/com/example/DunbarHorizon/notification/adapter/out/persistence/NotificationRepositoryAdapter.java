package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.notification.adapter.out.persistence.mongo.NotificationMongoRepository;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationMongoRepository notificationMongoRepository;

    @Override
    public Slice<Notification> findAllByReceiverId(Long receiverId, Pageable pageable) {
        return notificationMongoRepository.findAllByReceiverIdOrderByCreatedAtDesc(receiverId, pageable);
    }

    @Override
    public long countByReceiverIdAndIsReadFalse(Long receiverId) {
        return notificationMongoRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    @Override
    public List<Notification> findByIsSentFalse() {
        return notificationMongoRepository.findByIsSentFalse();
    }

    @Override
    public void deleteByCreatedAtBefore(LocalDateTime threshold) {
        notificationMongoRepository.deleteByCreatedAtBefore(threshold);
    }

    @Override
    public Notification save(Notification notification) {
        return notificationMongoRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(String id) {
        return notificationMongoRepository.findById(id);
    }

    @Override
    public List<Notification> saveAll(List<Notification> notifications) {
        return notificationMongoRepository.saveAll(notifications);
    }
}
