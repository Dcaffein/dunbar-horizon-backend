package com.example.DunbarHorizon.social.adapter.in.web;

import com.example.DunbarHorizon.global.annotation.CurrentUserId;
import com.example.DunbarHorizon.social.adapter.in.web.dto.FriendUpdateRequest;
import com.example.DunbarHorizon.social.application.dto.result.FriendshipDetailResult;
import com.example.DunbarHorizon.social.application.port.in.FriendshipCommandUseCase;
import com.example.DunbarHorizon.social.application.port.in.FriendshipQueryUseCase;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipCommandUseCase friendshipCommandUseCase;
    private final FriendshipQueryUseCase friendshipQueryUseCase;

    @GetMapping
    public ResponseEntity<List<FriendshipDetailResult>> getAllFriends(
            @CurrentUserId Long currentUserId) {

        return ResponseEntity.ok(friendshipQueryUseCase.getDetailedFriendships(currentUserId));
    }

    @PatchMapping("/{friendId}")
    public ResponseEntity<Void> updateFriend(
            @CurrentUserId Long currentUserId,
            @PathVariable Long friendId,
            @RequestBody @Valid FriendUpdateRequest friendUpdateRequest) {

        friendshipCommandUseCase.updateFriendship(currentUserId, friendId, friendUpdateRequest.toCommand(currentUserId, friendId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> brokeUpWithFriend(
            @CurrentUserId Long currentUserId,
            @PathVariable Long friendId) {

        friendshipCommandUseCase.brokeUpWith(currentUserId, friendId);
        return ResponseEntity.noContent().build();
    }
}