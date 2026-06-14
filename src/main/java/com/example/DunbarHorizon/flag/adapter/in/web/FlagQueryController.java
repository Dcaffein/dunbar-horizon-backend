package com.example.DunbarHorizon.flag.adapter.in.web;

import com.example.DunbarHorizon.flag.application.dto.result.FlagDetailResult;
import com.example.DunbarHorizon.flag.application.dto.result.FlagResult;
import com.example.DunbarHorizon.flag.application.port.in.FlagQueryUseCase;
import com.example.DunbarHorizon.flag.application.port.in.FlagRole;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flags")
@RequiredArgsConstructor
public class FlagQueryController {

    private final FlagQueryUseCase flagQueryUseCase;

    @GetMapping("/me")
    public ResponseEntity<List<FlagResult>> getMyFlagsByRole(
            @CurrentUserId Long currentUserId,
            @RequestParam FlagRole role
    ) {
        return ResponseEntity.ok(flagQueryUseCase.getFlagsByRole(currentUserId, role));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<FlagResult>> getUserFlagsByRole(
            @PathVariable Long userId,
            @RequestParam FlagRole role
    ) {
        return ResponseEntity.ok(flagQueryUseCase.getFlagsByRole(userId, role));
    }

    @GetMapping("/users/{userId}/recent")
    public ResponseEntity<List<FlagResult>> getRecentFlags(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(flagQueryUseCase.getRecentFlags(userId));
    }

    @GetMapping("/friends")
    public ResponseEntity<List<FlagResult>> getFriendFlags(
            @CurrentUserId Long currentUserId
    ) {
        return ResponseEntity.ok(flagQueryUseCase.getFriendFlags(currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlagDetailResult> getFlagDetail(
            @PathVariable Long id,
            @CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(flagQueryUseCase.getFlagDetail(id, currentUserId));
    }
}
