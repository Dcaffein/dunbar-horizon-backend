package com.example.DunbarHorizon.account.adapter.in.web;

import com.example.DunbarHorizon.account.application.dto.MyProfileResult;
import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.account.adapter.in.web.dto.UserProfileUpdateRequest;
import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.account.application.port.in.UserProfileUpdateUseCase;
import com.example.DunbarHorizon.account.application.port.out.ProfileImageStoragePort;
import com.example.DunbarHorizon.account.domain.exception.UserNotFoundException;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.global.imageStorage.PresignedUploadResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryUseCase userQueryUseCase;
    private final UserProfileUpdateUseCase userProfileUpdateUseCase;
    private final ProfileImageStoragePort profileImageStoragePort;

    @GetMapping("/me")
    public ResponseEntity<MyProfileResult> getMyProfile(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(userQueryUseCase.getMyProfile(currentUserId));
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> updateProfile(
            @CurrentUserId Long userId,
            @RequestBody @Valid UserProfileUpdateRequest request) {
        userProfileUpdateUseCase.updateProfile(userId, request.nickname(), request.profileImageKey());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/profile-image/presign")
    public ResponseEntity<PresignedUploadResult> presignProfileImage(
            @CurrentUserId Long userId,
            @RequestParam String contentType) {
        return ResponseEntity.ok(profileImageStoragePort.presignUpload(contentType));
    }

    @GetMapping("/search")
    public ResponseEntity<UserProfileInfo> searchByEmail(
            @CurrentUserId Long currentUserId,
            @RequestParam String email) {

        UserProfileInfo result = userQueryUseCase.findActiveUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("해당 이메일로 등록된 사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(result);
    }
}
