package com.example.GooRoomBe.account.adapter.out.persistence.jpa;

import com.example.GooRoomBe.account.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

    @Query("SELECT r FROM RefreshToken r WHERE r.tokenValue = :tokenValue AND r.expiryDate > :now")
    Optional<RefreshToken> findValidToken(@Param("tokenValue") String tokenValue, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    void deleteByTokenValue(String tokenValue);
}
