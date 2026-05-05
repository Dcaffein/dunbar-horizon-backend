package com.example.DunbarHorizon.notification.adapter.in.web.dto;

import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.domain.Notification;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationResponse(
        String id,
        String title,
        String content,
        Map<String, Object> metadata,
        NotificationType type,
        boolean isRead,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getMetadata(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}