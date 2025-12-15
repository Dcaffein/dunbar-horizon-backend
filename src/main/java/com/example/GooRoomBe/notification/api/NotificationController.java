package com.example.GooRoomBe.notification.api;

import com.example.GooRoomBe.notification.api.dto.DeviceTokenRequestDto;
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

    /**
     * FCM 디바이스 토큰 등록
     * URL: POST /api/v1/notifications/device-token
     */
    @PostMapping("/device-token")
    public ResponseEntity<Void> registerDeviceToken(
            @AuthenticationPrincipal String userId,
            @RequestBody DeviceTokenRequestDto dto) { // DTO는 record로 간단히 생성

        notificationService.registerDeviceToken(userId, dto.token());
        return ResponseEntity.ok().build();
    }
}

// DTO (같은 파일이나 별도 파일)
// record DeviceTokenRequestDto(@NotBlank String token) {}