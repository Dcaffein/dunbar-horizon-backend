package com.example.GooRoomBe.notification.domain;

import com.example.GooRoomBe.global.event.NotificationType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Node("Notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    private String receiverId;

    private String title;

    private String content;

    private String relatedUrl;

    private NotificationType type;

    private boolean isRead;

    private boolean isSent;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Notification(String receiverId, String title, String content, String relatedUrl, NotificationType type, boolean isSent) {
        this.id = UUID.randomUUID().toString();
        this.receiverId = receiverId;
        this.title = title;
        this.content = content;
        this.relatedUrl = relatedUrl;
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