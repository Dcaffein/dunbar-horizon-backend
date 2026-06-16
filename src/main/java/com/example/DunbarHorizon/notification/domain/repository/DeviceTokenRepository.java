package com.example.DunbarHorizon.notification.domain.repository;

import com.example.DunbarHorizon.notification.domain.DeviceToken;

import java.util.List;

public interface DeviceTokenRepository {
    DeviceToken save(DeviceToken deviceToken);
    boolean existsByFcmToken(String fcmToken);
    void deleteByFcmToken(String fcmToken);
    void deleteAllByFcmTokenIn(List<String> fcmTokens);
    List<String> findAllFcmTokensByUserIdIn(List<Long> userIds);
    boolean existsByUserIdAndFcmToken(Long userId, String fcmToken);
    void deleteAllByUserId(Long userId);
}
