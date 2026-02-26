package com.example.GooRoomBe.account.domain.model;

import com.example.GooRoomBe.account.domain.exception.AlreadyRegisteredEmailException;
import com.example.GooRoomBe.global.common.BaseTimeAggregateRoot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "auths", indexes = {
        @Index(name = "idx_auth_user_provider", columnList = "user_id, provider")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auth extends BaseTimeAggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    private String password;

    private String providerId;

    @Column(nullable = false)
    private boolean verified;

    @Builder
    private Auth(Long userId, AuthProvider provider, String password, String providerId, boolean verified) {
        this.userId = userId;
        this.provider = provider;
        this.password = password;
        this.providerId = providerId;
        this.verified = verified;
    }

    public void overwritePassword(String encodedPassword, String email) {
        if (this.verified) {
            throw new AlreadyRegisteredEmailException(email);
        }
        if (this.provider == AuthProvider.LOCAL) {
            this.password = encodedPassword;
        }
    }

    public static Auth createLocalAuth(Long userId, String encodedPassword) {
        return Auth.builder()
                .userId(userId)
                .provider(AuthProvider.LOCAL)
                .password(encodedPassword)
                .verified(false)
                .build();
    }


    public static Auth createOAuth(Long userId, AuthProvider provider, String providerId) {
        Auth auth = Auth.builder()
                .userId(userId)
                .provider(provider)
                .providerId(providerId)
                .verified(false)
                .build();

        auth.verify();
        return auth;
    }

    public void verify() {
        this.verified = true;
    }
}