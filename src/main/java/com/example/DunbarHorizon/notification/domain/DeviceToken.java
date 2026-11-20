package com.example.DunbarHorizon.notification.domain;

import com.example.DunbarHorizon.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "device_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken extends BaseTimeEntity {

    @Id
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public DeviceToken(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }
}
