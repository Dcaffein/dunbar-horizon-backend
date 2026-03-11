package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.notification.adapter.out.persistence.jpa.NotificationSettingJpaRepository;
import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import com.example.DunbarHorizon.notification.domain.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationSettingRepositoryAdapter implements NotificationSettingRepository {

    private final NotificationSettingJpaRepository notificationSettingJpaRepository;

    @Override
    public Optional<NotificationSetting> findById(Long userId) {
        return notificationSettingJpaRepository.findById(userId);
    }

    @Override
    public NotificationSetting save(NotificationSetting setting) {
        return notificationSettingJpaRepository.save(setting);
    }

    @Override
    public List<NotificationSetting> findAllByUserIdIn(List<Long> userIds) {
        return notificationSettingJpaRepository.findAllByUserIdIn(userIds);
    }
}
