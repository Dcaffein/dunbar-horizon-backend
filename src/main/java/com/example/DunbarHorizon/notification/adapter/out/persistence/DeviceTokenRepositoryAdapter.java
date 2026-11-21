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
    public void deleteByToken(String token) {
        deviceTokenJpaRepository.deleteByToken(token);
    }

    @Override
    public int deleteByUserIdAndToken(Long userId, String token) {
        return deviceTokenJpaRepository.deleteByUserIdAndToken(userId, token);
    }

    @Override
    public void deleteAllByTokenIn(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return;
        deviceTokenJpaRepository.deleteAllByTokenIn(tokens);
    }

    @Override
    public List<String> findAllTokensByUserIdIn(List<Long> userIds) {
        return deviceTokenJpaRepository.findAllTokensByUserIdIn(userIds);
    }

    @Override
    public boolean existsByUserIdAndToken(Long userId, String token) {
        return deviceTokenJpaRepository.existsByUserIdAndToken(userId, token);
    }
}
