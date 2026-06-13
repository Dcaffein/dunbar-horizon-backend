package com.example.DunbarHorizon.flag.adapter.in.web;

import com.example.DunbarHorizon.flag.application.port.in.FlagInvitationUseCase;
import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/flag-invitations")
@RequiredArgsConstructor
public class FlagInvitationController {

    private final FlagInvitationUseCase flagInvitationUseCase;

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<Void> accept(
            @PathVariable Long invitationId,
            @CurrentUserId Long currentUserId
    ) {
        flagInvitationUseCase.accept(invitationId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long invitationId,
            @CurrentUserId Long currentUserId
    ) {
        flagInvitationUseCase.reject(invitationId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long invitationId,
            @CurrentUserId Long currentUserId
    ) {
        flagInvitationUseCase.cancel(invitationId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
