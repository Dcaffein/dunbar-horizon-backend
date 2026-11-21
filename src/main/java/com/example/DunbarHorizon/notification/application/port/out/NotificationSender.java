package com.example.DunbarHorizon.notification.application.port.out;

import java.util.List;

public interface NotificationSender {
    List<String> send(PushMessage message, List<String> tokens);
}
