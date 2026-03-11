package com.example.DunbarHorizon.notification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_settings",
        indexes = @Index(name = "idx_notification_user_id", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

    @Id
    @Column(name = "user_id")
    private Long userId;

    private String fcmToken;

    private boolean isAlarmOn;

    public NotificationSetting(Long userId, String fcmToken) {
        this.userId = userId;
        this.fcmToken = fcmToken;
        this.isAlarmOn = true;
    }

    public void updateToken(String newToken) {
        this.fcmToken = newToken;
    }

    public void toggleAlarm(boolean isOn) {
        this.isAlarmOn = isOn;
    }
}