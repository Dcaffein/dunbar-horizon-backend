package com.example.GooRoomBe.account.adapter.out.persistence.jpa;

import com.example.GooRoomBe.account.domain.model.Auth;
import com.example.GooRoomBe.account.domain.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthJpaRepository extends JpaRepository<Auth, Long> {
    Optional<Auth> findByUserIdAndProvider(Long userId, AuthProvider provider);
    boolean existsByUserIdAndProviderAndProviderId(Long userId, AuthProvider provider, String providerId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Auth a WHERE a.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Auth a WHERE a.userId = :userId AND a.verified = false")
    void deleteUnverifiedByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Auth a WHERE a.verified = false AND a.createdAt < :threshold")
    int deleteOldUnverifiedAuths(@Param("threshold") LocalDateTime threshold);
}
