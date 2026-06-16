package com.example.DunbarHorizon.notification.adapter.in.web;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Profile("local")
@RestController
@RequestMapping("/api/dev/notifications")
@RequiredArgsConstructor
public class NotificationDevController {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * POST /api/dev/notifications/send?receiverIds=1,2&type=TRACE_REVEALED
     * NotificationEvent를 직접 발행해 FCM 전송 파이프라인 전체를 테스트한다.
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @RequestParam List<Long> receiverIds,
            @RequestParam(defaultValue = "TRACE_REVEALED") NotificationType type) {

        eventPublisher.publishEvent(NotificationEvent.builder()
                .receiverIds(receiverIds)
                .title("[DEV] 테스트 알림")
                .content("FCM 파이프라인 테스트 메시지입니다.")
                .type(type)
                .build());

        return ResponseEntity.ok(Map.of(
                "message", "NotificationEvent published",
                "receiverIds", receiverIds,
                "type", type.name()
        ));
    }
}
