package com.example.DunbarHorizon.notification.adapter.in.web;

import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.notification.adapter.in.web.dto.DeviceTokenRequest;
import com.example.DunbarHorizon.notification.adapter.in.web.dto.DeviceTokenStatusResponse;
import com.example.DunbarHorizon.notification.adapter.in.web.dto.NotificationResponse;
import com.example.DunbarHorizon.notification.application.NotificationService;
import com.example.DunbarHorizon.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/device-token")
    public ResponseEntity<Void> registerDeviceToken(
            @CurrentUserId Long currentUserId,
            @RequestBody DeviceTokenRequest dto) {
        notificationService.registerDeviceToken(currentUserId, dto.token());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/device-token")
    public ResponseEntity<Void> removeDeviceToken(
            @CurrentUserId Long currentUserId) {
        notificationService.removeDeviceTokenByUserId(currentUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/device-token/status")
    public ResponseEntity<DeviceTokenStatusResponse> getDeviceTokenStatus(
            @CurrentUserId Long currentUserId,
            @RequestParam String token) {
        boolean registered = notificationService.isTokenRegisteredForUser(currentUserId, token);
        return ResponseEntity.ok(new DeviceTokenStatusResponse(registered));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> readNotification(
            @PathVariable String notificationId,
            @CurrentUserId Long currentUserId) {
        Notification updatedNotification = notificationService.readNotification(notificationId, currentUserId);
        return ResponseEntity.ok(NotificationResponse.from(updatedNotification));
    }

    @GetMapping
    public ResponseEntity<Slice<NotificationResponse>> getMyNotifications(
            @CurrentUserId Long currentUserId,
            @PageableDefault(size = 20) Pageable pageable) {
        Slice<NotificationResponse> responses = notificationService
                .getMyNotifications(currentUserId, pageable)
                .map(NotificationResponse::from);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@CurrentUserId Long currentUserId) {
        long unreadCount = notificationService.countUnreadNotifications(currentUserId);
        return ResponseEntity.ok(unreadCount);
    }
}
