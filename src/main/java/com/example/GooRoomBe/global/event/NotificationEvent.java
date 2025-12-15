package com.example.GooRoomBe.global.event;

import java.time.LocalDateTime;


public record NotificationEvent(
        String receiverId,
        String title,
        String content,
        String relatedUrl,
        NotificationType type,
        LocalDateTime occurredAt
) {
    public NotificationEvent(String receiverId, String title, String content, String relatedUrl, NotificationType type) {
        this(receiverId, title, content, relatedUrl, type, LocalDateTime.now());
    }

    public static NotificationEvent toAll(String title, String content, String url) {
        return new NotificationEvent(
                "ALL",
                title,
                content,
                url,
                NotificationType.NOTICE
        );
    }
}