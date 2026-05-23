package com.example.DunbarHorizon.account.adapter.in.web;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.account.domain.exception.UserNotFoundException;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryUseCase userQueryUseCase;

    @GetMapping("/search")
    public ResponseEntity<UserProfileInfo> searchByEmail(
            @CurrentUserId Long currentUserId,
            @RequestParam String email) {

        UserProfileInfo result = userQueryUseCase.findActiveUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("해당 이메일로 등록된 사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(result);
    }
}
