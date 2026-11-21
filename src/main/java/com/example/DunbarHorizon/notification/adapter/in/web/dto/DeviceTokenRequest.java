package com.example.DunbarHorizon.notification.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenRequest(
        @NotBlank(message = "디바이스 토큰은 필수입니다.")
        @Size(max = 255, message = "디바이스 토큰은 255자 이하여야 합니다.")
        String token
) {
}
