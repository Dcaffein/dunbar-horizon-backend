package com.example.DunbarHorizon.notification.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.notification.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeviceTokenJpaRepository extends JpaRepository<DeviceToken, String> {

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token = :token")
    void deleteByToken(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.userId = :userId AND d.token = :token")
    void deleteByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token IN :tokens")
    void deleteAllByTokenIn(@Param("tokens") List<String> tokens);

    @Query("SELECT d.token FROM DeviceToken d WHERE d.userId IN :userIds")
    List<String> findAllTokensByUserIdIn(@Param("userIds") List<Long> userIds);

    boolean existsByUserIdAndToken(Long userId, String token);
}
