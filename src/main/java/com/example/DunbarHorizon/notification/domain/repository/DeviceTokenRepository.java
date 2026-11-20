package com.example.DunbarHorizon.notification.domain.repository;

import com.example.DunbarHorizon.notification.domain.DeviceToken;

import java.util.List;

public interface DeviceTokenRepository {
    DeviceToken save(DeviceToken deviceToken);
    boolean existsByToken(String token);
    void deleteByToken(String token);
    void deleteAllByTokenIn(List<String> tokens);
    List<String> findAllTokensByUserIdIn(List<Long> userIds);
    boolean existsByUserIdAndToken(Long userId, String token);
    void deleteAllByUserId(Long userId);
}
