package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.notification.domain.DeviceToken;
import com.example.DunbarHorizon.notification.domain.event.DeviceTokenRegisteredEvent;
import com.example.DunbarHorizon.notification.domain.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void registerDeviceToken(Long userId, String token) {
        if (deviceTokenRepository.existsByUserIdAndFcmToken(userId, token)) {
            return;
        }
        deviceTokenRepository.deleteAllByUserId(userId);
        deviceTokenRepository.save(new DeviceToken(userId, token));
        eventPublisher.publishEvent(new DeviceTokenRegisteredEvent(token));
    }

    @Transactional
    public void removeDeviceToken(String fcmToken) {
        deviceTokenRepository.deleteByFcmToken(fcmToken);
    }

    @Transactional
    public void removeDeviceTokenByUserId(Long userId) {
        deviceTokenRepository.deleteAllByUserId(userId);
    }

    public boolean isTokenRegisteredForUser(Long userId, String fcmToken) {
        return deviceTokenRepository.existsByUserIdAndFcmToken(userId, fcmToken);
    }

    public List<String> findFcmTokensByUserIds(List<Long> userIds) {
        return deviceTokenRepository.findAllFcmTokensByUserIdIn(userIds);
    }

    @Transactional
    public void cleanupInvalidTokens(List<String> invalidTokens) {
        if (invalidTokens == null || invalidTokens.isEmpty()) return;
        deviceTokenRepository.deleteAllByFcmTokenIn(invalidTokens);
    }
}
