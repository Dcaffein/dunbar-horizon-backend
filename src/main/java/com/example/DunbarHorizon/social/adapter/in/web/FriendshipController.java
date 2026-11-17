package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.social.adapter.in.web.dto.FriendUpdateRequest;
import com.example.DunbarHorizon.social.application.dto.result.FriendshipDetailResult;
import com.example.DunbarHorizon.social.application.service.FriendshipCommandService;
import com.example.DunbarHorizon.social.application.service.FriendshipQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipCommandService friendshipCommandService;
    private final FriendshipQueryService friendshipQueryService;

    @GetMapping
    public ResponseEntity<List<FriendshipDetailResult>> getAllFriends(
            @CurrentUserId Long currentUserId) {

        return ResponseEntity.ok(friendshipQueryService.getDetailedFriendships(currentUserId));
    }

    @GetMapping("/{friendId}")
    public ResponseEntity<FriendshipDetailResult> getFriendDetails(
            @CurrentUserId Long currentUserId,
            @PathVariable Long friendId
    ) {
        return ResponseEntity.ok(friendshipQueryService.getFriend(currentUserId, friendId));
    }

    @PatchMapping("/{friendId}")
    public ResponseEntity<Void> updateFriend(
            @CurrentUserId Long currentUserId,
            @PathVariable Long friendId,
            @RequestBody @Valid FriendUpdateRequest friendUpdateRequest) {

        friendshipCommandService.updateFriendship(currentUserId, friendId, friendUpdateRequest.toCommand(currentUserId, friendId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> brokeUpWithFriend(
            @CurrentUserId Long currentUserId,
            @PathVariable Long friendId) {

        friendshipCommandService.brokeUpWith(currentUserId, friendId);
        return ResponseEntity.noContent().build();
    }
}