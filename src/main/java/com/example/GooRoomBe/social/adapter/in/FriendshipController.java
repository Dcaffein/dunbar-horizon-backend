package com.example.GooRoomBe.social.adapter.in;

import com.example.GooRoomBe.global.annotation.CurrentUserId;
import com.example.GooRoomBe.social.adapter.in.dto.FriendUpdateRequest;
import com.example.GooRoomBe.social.application.port.in.FriendshipCommandUseCase;
import com.example.GooRoomBe.social.application.port.in.FriendshipQueryUseCase;
import com.example.GooRoomBe.social.application.port.in.dto.FriendProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipCommandUseCase friendshipCommandUseCase;
    private final FriendshipQueryUseCase friendshipQueryUseCase;

    @GetMapping
    public ResponseEntity<Set<FriendProfile>> getAllFriends(
            @CurrentUserId Long currentUserId) {

        return ResponseEntity.ok(friendshipQueryUseCase.getAllFriends(currentUserId));
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