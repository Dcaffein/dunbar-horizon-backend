package com.example.DunbarHorizon.notification.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.notification.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeviceTokenJpaRepository extends JpaRepository<DeviceToken, Long> {

    boolean existsByFcmToken(String fcmToken);

    void deleteByFcmToken(String fcmToken);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.fcmToken IN :fcmTokens")
    void deleteAllByFcmTokenIn(@Param("fcmTokens") List<String> fcmTokens);

    @Query("SELECT d.fcmToken FROM DeviceToken d WHERE d.userId IN :userIds")
    List<String> findAllFcmTokensByUserIdIn(@Param("userIds") List<Long> userIds);

    boolean existsByUserIdAndFcmToken(Long userId, String fcmToken);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
