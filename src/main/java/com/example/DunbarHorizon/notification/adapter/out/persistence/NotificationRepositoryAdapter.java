package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.notification.adapter.out.persistence.jpa.NotificationJpaRepository;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public Slice<Notification> findAllByReceiverId(Long receiverId, Pageable pageable) {
        return notificationJpaRepository.findAllByReceiverIdOrderByCreatedAtDesc(receiverId, pageable);
    }

    @Override
    public long countByReceiverIdAndIsReadFalse(Long receiverId) {
        return notificationJpaRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    @Override
    public Notification save(Notification notification) {
        return notificationJpaRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notificationJpaRepository.findById(id);
    }

    @Override
    public List<Notification> saveAll(List<Notification> notifications) {
        return notificationJpaRepository.saveAll(notifications);
    }

    @Override
    public void deleteById(Long id) {
        notificationJpaRepository.deleteById(id);
    }
}
