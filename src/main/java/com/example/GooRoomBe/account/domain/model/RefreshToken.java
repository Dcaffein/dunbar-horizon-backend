package com.example.GooRoomBe.account.domain.model;

import com.example.GooRoomBe.global.common.BaseTimeAggregateRoot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_rt_user_id", columnList = "userId")
})
public class RefreshToken extends BaseTimeAggregateRoot {
    private static final long REFRESH_TOKEN_EXPIRATION_TIME_VALUE = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String tokenValue;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Builder
    public RefreshToken(Long userId, String tokenValue) {
        this.userId = userId;
        this.tokenValue = tokenValue;
        this.expiryDate =  LocalDateTime.now().plusHours(REFRESH_TOKEN_EXPIRATION_TIME_VALUE);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    public void rotateTokenValue(String newRefreshToken) {
        this.tokenValue = newRefreshToken;
    }
}