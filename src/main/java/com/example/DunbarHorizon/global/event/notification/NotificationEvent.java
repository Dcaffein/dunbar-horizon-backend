package com.example.DunbarHorizon.global.event.notification;

import lombok.Builder;
import lombok.Singular;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
public record NotificationEvent(
        TargetType targetType,
        @Singular("receiverId") List<Long> receiverIds,
        String title,
        String content,
        NotificationType type,
        Map<String, Object> metadata,
        LocalDateTime occurredAt
) {
    public enum TargetType {
        SPECIFIC_USERS, BROADCAST
    }

    public NotificationEvent {
        if (occurredAt == null) occurredAt = LocalDateTime.now();

        // 타겟 타입이 설정되지 않았다면 기본값으로 특정 사용자 발송 지정
        if (targetType == null) targetType = TargetType.SPECIFIC_USERS;

        // 특정 사용자 발송일 때만 수신자 목록 검증 (브로드캐스트는 수신자 목록이 필요 없음)
        if (targetType == TargetType.SPECIFIC_USERS && (receiverIds == null || receiverIds.isEmpty())) {
            throw new IllegalArgumentException("특정 사용자 발송 시 알림 수신자(receiverIds)는 최소 한 명 이상이어야 합니다.");
        }
    }

    public NotificationEvent(Long receiverId, String title, String content,
                             NotificationType type, Map<String, Object> metadata) {
        this(TargetType.SPECIFIC_USERS, List.of(receiverId), title, content, type, metadata, LocalDateTime.now());
    }

    public static NotificationEvent toAll(String title, String content, NotificationType type, Map<String, Object> metadata) {
        return NotificationEvent.builder()
                .targetType(TargetType.BROADCAST)
                .title(title)
                .content(content)
                .type(type)
                .metadata(metadata)
                .build();
    }

    public boolean isAnnouncement() {
        return this.targetType == TargetType.BROADCAST;
    }
}