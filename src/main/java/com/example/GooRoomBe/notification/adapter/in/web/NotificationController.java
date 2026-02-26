package com.example.GooRoomBe.notification.adapter.in.web;

import com.example.GooRoomBe.global.annotation.CurrentUserId;
import com.example.GooRoomBe.notification.adapter.in.web.dto.DeviceTokenRequestDto;
import com.example.GooRoomBe.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/device-token")
    public ResponseEntity<Void> registerDeviceToken(
            @CurrentUserId Long currentUserId,
            @RequestBody DeviceTokenRequestDto dto) {

        notificationService.registerDeviceToken(currentUserId, dto.token());
        return ResponseEntity.ok().build();
    }
}
