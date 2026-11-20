package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.notification.adapter.out.persistence.jpa.DeviceTokenJpaRepository;
import com.example.DunbarHorizon.notification.domain.DeviceToken;
import com.example.DunbarHorizon.notification.domain.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DeviceTokenRepositoryAdapter implements DeviceTokenRepository {

    private final DeviceTokenJpaRepository deviceTokenJpaRepository;

    @Override
    public DeviceToken save(DeviceToken deviceToken) {
        return deviceTokenJpaRepository.save(deviceToken);
    }

    @Override
    public boolean existsByFcmToken(String fcmToken) {
        return deviceTokenJpaRepository.existsByToken(fcmToken);
    }

    @Override
    public void deleteByFcmToken(String fcmToken) {
        deviceTokenJpaRepository.deleteByToken(fcmToken);
    }

    @Override
    public void deleteAllByFcmTokenIn(List<String> fcmTokens) {
        if (fcmTokens == null || fcmTokens.isEmpty()) return;
        deviceTokenJpaRepository.deleteAllByTokenIn(fcmTokens);
    }

    @Override
    public List<String> findAllFcmTokensByUserIdIn(List<Long> userIds) {
        return deviceTokenJpaRepository.findAllTokensByUserIdIn(userIds);
    }

    @Override
    public boolean existsByUserIdAndFcmToken(Long userId, String fcmToken) {
        return deviceTokenJpaRepository.existsByUserIdAndToken(userId, fcmToken);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        deviceTokenJpaRepository.deleteAllByUserId(userId);
    }
}
