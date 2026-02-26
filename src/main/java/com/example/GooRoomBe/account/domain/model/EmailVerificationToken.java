package com.example.GooRoomBe.account.domain.model;

import com.example.GooRoomBe.global.common.BaseTimeAggregateRoot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verification_tokens", indexes = {
        @Index(name = "idx_token_user_id", columnList = "user_id")
})
public class EmailVerificationToken extends BaseTimeAggregateRoot {

    private static final long EMAIL_TOKEN_EXPIRATION_TIME_VALUE = 24L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    public EmailVerificationToken(User user) {
        this.user = user;
        this.expiryDate = LocalDateTime.now().plusHours(EMAIL_TOKEN_EXPIRATION_TIME_VALUE);
        this.token = UUID.randomUUID().toString();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}