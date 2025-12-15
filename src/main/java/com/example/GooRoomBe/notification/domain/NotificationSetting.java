package com.example.GooRoomBe.notification.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("NotificationSetting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

    @Id
    private String userId;

    private String fcmToken;

    private boolean isAlarmOn;

    public NotificationSetting(String userId, String fcmToken) {
        this.userId = userId;
        this.fcmToken = fcmToken;
        this.isAlarmOn = true; // 기본값 ON
    }

    public void updateToken(String newToken) {
        this.fcmToken = newToken;
    }

    public void toggleAlarm(boolean isOn) {
        this.isAlarmOn = isOn;
    }
}