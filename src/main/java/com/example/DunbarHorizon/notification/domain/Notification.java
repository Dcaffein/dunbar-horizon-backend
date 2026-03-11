package com.example.DunbarHorizon.notification.domain;

import com.example.DunbarHorizon.global.event.notification.NotificationType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "notifications")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    private String id;

    @Indexed
    private Long receiverId;

    private String title;

    private String content;

    private Map<String, Object> metadata;

    private NotificationType type;

    private boolean isRead;

    private boolean isSent;

    @CreatedDate
    @Indexed(expireAfter = "30d")
    private LocalDateTime createdAt;

    public Notification(Long receiverId, String title, String content,
                        Map<String, Object> metadata, NotificationType type, boolean isSent) {
        this.receiverId = receiverId;
        this.title = title;
        this.content = content;
        this.metadata = metadata;
        this.type = type;
        this.isSent = isSent;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public void read() {
        this.isRead = true;
    }

    public void markAsSent() {
        this.isSent = true;
    }
}