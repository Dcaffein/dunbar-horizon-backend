package com.example.DunbarHorizon.notification.application.port.out;

import java.util.Map;

public record PushMessage(String title, String body, Map<String, Object> data) {
}
