package com.example.GooRoomBe.global.event.notification;

import lombok.Builder;
import lombok.Singular;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
public record NotificationEvent(
        @Singular("receiverId") List<Long> receiverIds,
        String title,
        String content,
        NotificationType type,
        Map<String, Object> metadata,
        LocalDateTime occurredAt
) {

    public NotificationEvent {
        if (occurredAt == null) occurredAt = LocalDateTime.now();
        if (receiverIds == null || receiverIds.isEmpty()) {
            throw new IllegalArgumentException("알림 수신자(receiverIds)는 최소 한 명 이상이어야 합니다.");
        }
    }

    public NotificationEvent(Long receiverId, String title, String content,
                             NotificationType type, Map<String, Object> metadata) {
        this(List.of(receiverId), title, content, type, metadata, LocalDateTime.now());
    }

    public static final Long ALL_RECEIVERS = 0L;

    public static NotificationEvent toAll(String title, String content, NotificationType type, Map<String, Object> metadata) {
        return NotificationEvent.builder()
                .receiverId(ALL_RECEIVERS)
                .title(title)
                .content(content)
                .type(type)
                .metadata(metadata)
                .build();
    }

    public boolean isAnnouncement() {
        return receiverIds != null && receiverIds.size() == 1 && ALL_RECEIVERS.equals(receiverIds.get(0));
    }
}