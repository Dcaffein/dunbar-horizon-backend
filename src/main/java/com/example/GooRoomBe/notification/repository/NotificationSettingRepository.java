package com.example.GooRoomBe.notification.repository;

import com.example.GooRoomBe.notification.domain.NotificationSetting;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository {
    Optional<NotificationSetting> findById(Long userId);
    NotificationSetting save(NotificationSetting setting);
    List<NotificationSetting> findAllByUserIdIn(List<Long> userIds);
}