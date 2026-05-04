package com.example.DunbarHorizon.notification.application.port.out;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import java.util.List;

public interface NotificationSender {
    List<String> sendMulticast(NotificationEvent event, List<String> tokens);
    void sendBroadcast(NotificationEvent event);
    void subscribeToTopic(String token, String topic);
    void unsubscribeFromTopic(String token, String topic);
}